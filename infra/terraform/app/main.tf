# App stack: resources that are DESTROYED nightly (cost).
# Reads the foundation resources by name, so it does not depend on where the foundation state lives.
# Apply foundation/ first.

data "azurerm_client_config" "current" {}

data "azurerm_resource_group" "this" {
  name = var.resource_group_name
}

data "azurerm_user_assigned_identity" "api" {
  name                = "id-${var.workload}-api-${var.environment}-${var.region_abbr}"
  resource_group_name = data.azurerm_resource_group.this.name
}

data "azurerm_user_assigned_identity" "func" {
  name                = "id-${var.workload}-func-${var.environment}-${var.region_abbr}"
  resource_group_name = data.azurerm_resource_group.this.name
}

data "azurerm_application_insights" "this" {
  name                = "appi-${var.workload}-${var.environment}-${var.region_abbr}"
  resource_group_name = data.azurerm_resource_group.this.name
}

data "azurerm_key_vault" "this" {
  name                = "kv-${var.workload}-${var.environment}-${var.region_abbr}"
  resource_group_name = data.azurerm_resource_group.this.name
}

locals {
  tags = {
    owner   = var.owner
    purpose = "Onboarding platform learning project"
    expiry  = var.expiry
  }
}

resource "azurerm_service_plan" "this" {
  name                = "asp-${var.workload}-${var.environment}-${var.region_abbr}"
  location            = data.azurerm_resource_group.this.location
  resource_group_name = data.azurerm_resource_group.this.name
  os_type             = "Linux"
  sku_name            = "B1"
  tags                = local.tags
}

resource "azurerm_linux_web_app" "api" {
  name                = "app-${var.workload}-${var.environment}-${var.region_abbr}" # globally unique
  location            = data.azurerm_resource_group.this.location
  resource_group_name = data.azurerm_resource_group.this.name
  service_plan_id     = azurerm_service_plan.this.id
  https_only          = true
  tags                = local.tags

  identity {
    type         = "UserAssigned"
    identity_ids = [data.azurerm_user_assigned_identity.api.id]
  }

  site_config {
    ftps_state                        = "Disabled"
    health_check_path                 = "/actuator/health/readiness"
    health_check_eviction_time_in_min = 2

    application_stack {
      java_server         = "JAVA" # Java SE: runs your Spring Boot JAR
      java_server_version = "21"
      java_version        = "21"
    }
  }

  app_settings = {
    KEYVAULT_ENABLED        = "true"
    AZURE_KEYVAULT_ENDPOINT = data.azurerm_key_vault.this.vault_uri
    AZURE_CLIENT_ID         = data.azurerm_user_assigned_identity.api.client_id
    SERVICEBUS_ENABLED      = "true"
    SERVICEBUS_NAMESPACE    = azurerm_servicebus_namespace.this.name
    SERVICEBUS_QUEUE        = azurerm_servicebus_queue.application_submitted.name

    # Application Insights Java agent, attached by App Service at JVM start: no code change.
    ApplicationInsightsAgent_EXTENSION_VERSION        = "~3"
    APPLICATIONINSIGHTS_CONNECTION_STRING             = data.azurerm_application_insights.this.connection_string
    APPLICATIONINSIGHTS_AUTHENTICATION_STRING         = "Authorization=AAD;ClientId=${data.azurerm_user_assigned_identity.api.client_id}"
    APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL = "INFO" # logs sent to App Insights; WARN if volume grows
  }
}

# ---------------------------------------------------------------------------
# Service Bus
# Standard tier (topics and transactions are planned). Destroyed nightly with the rest of this
# stack: messages still in the queue are lost, which is acceptable for dev.
# ---------------------------------------------------------------------------

resource "azurerm_servicebus_namespace" "this" {
  name                = "sb-${var.workload}-${var.environment}-${var.region_abbr}" # globally unique
  location            = data.azurerm_resource_group.this.location
  resource_group_name = data.azurerm_resource_group.this.name
  sku                 = "Standard"
  local_auth_enabled  = false # no SAS keys / connection strings: Entra ID (managed identity, az login) only
  minimum_tls_version = "1.2"
  tags                = local.tags
}

resource "azurerm_servicebus_queue" "application_submitted" {
  name         = "application-submitted"
  namespace_id = azurerm_servicebus_namespace.this.id

  lock_duration                        = "PT1M" # time the consumer has before the message is unlocked again
  max_delivery_count                   = 5      # then the message goes to the dead-letter queue
  default_message_ttl                  = "P1D"  # the namespace does not live longer anyway
  dead_lettering_on_message_expiration = true

  # Can only be set at creation. The API uses applicationId as messageId, so a retried send
  # within the window is dropped by Service Bus.
  requires_duplicate_detection            = true
  duplicate_detection_history_time_window = "PT10M"
}

# The API may only send to the queue.
resource "azurerm_role_assignment" "api_sb_sender" {
  scope                = azurerm_servicebus_queue.application_submitted.id
  role_definition_name = "Azure Service Bus Data Sender"
  principal_id         = data.azurerm_user_assigned_identity.api.principal_id
}

# The Function may only receive from the queue.
resource "azurerm_role_assignment" "func_sb_receiver" {
  scope                = azurerm_servicebus_queue.application_submitted.id
  role_definition_name = "Azure Service Bus Data Receiver"
  principal_id         = data.azurerm_user_assigned_identity.func.principal_id
}

# Whoever runs Terraform: send, receive and peek from a laptop (az login) and Service Bus Explorer.
resource "azurerm_role_assignment" "deployer_sb_owner" {
  scope                = azurerm_servicebus_namespace.this.id
  role_definition_name = "Azure Service Bus Data Owner"
  principal_id         = data.azurerm_client_config.current.object_id
}

# ---------------------------------------------------------------------------
# Function App (onboarding-processor) on the existing B1 plan
# ---------------------------------------------------------------------------

# Required by the Functions runtime (host state, locks). Lowercase letters and digits only.
resource "azurerm_storage_account" "func" {
  name                     = "st${var.workload}${var.environment}${var.region_abbr}" # stivaninterviewdevbec, globally unique
  location                 = data.azurerm_resource_group.this.location
  resource_group_name      = data.azurerm_resource_group.this.name
  account_tier             = "Standard"
  account_replication_type = "LRS"
  min_tls_version          = "TLS1_2"
  tags                     = local.tags
}

# The Function reaches its storage with its identity instead of an account key.
resource "azurerm_role_assignment" "func_storage_blob_owner" {
  scope                = azurerm_storage_account.func.id
  role_definition_name = "Storage Blob Data Owner"
  principal_id         = data.azurerm_user_assigned_identity.func.principal_id
}

resource "azurerm_linux_function_app" "processor" {
  name                          = "func-${var.workload}-${var.environment}-${var.region_abbr}" # globally unique
  location                      = data.azurerm_resource_group.this.location
  resource_group_name           = data.azurerm_resource_group.this.name
  service_plan_id               = azurerm_service_plan.this.id # shares the B1 plan with the web app
  storage_account_name          = azurerm_storage_account.func.name
  storage_uses_managed_identity = true
  https_only                    = true
  tags                          = local.tags

  identity {
    type         = "UserAssigned"
    identity_ids = [data.azurerm_user_assigned_identity.func.id]
  }

  site_config {
    always_on  = true # on a dedicated plan the Service Bus trigger stops listening without it
    ftps_state = "Disabled"

    application_insights_connection_string = data.azurerm_application_insights.this.connection_string

    application_stack {
      java_version = "21"
    }
  }

  app_settings = {
    # Host storage via the user-assigned identity (AzureWebJobsStorage__accountName is set by the provider)
    AzureWebJobsStorage__credential = "managedidentity"
    AzureWebJobsStorage__clientId   = data.azurerm_user_assigned_identity.func.client_id

    # Identity-based Service Bus connection, used as connection = "ServiceBusConnection" in the trigger
    ServiceBusConnection__fullyQualifiedNamespace = "${azurerm_servicebus_namespace.this.name}.servicebus.windows.net"
    ServiceBusConnection__credential              = "managedidentity"
    ServiceBusConnection__clientId                = data.azurerm_user_assigned_identity.func.client_id

    SERVICEBUS_QUEUE = azurerm_servicebus_queue.application_submitted.name

    # Application Insights: log in with the identity (local auth is disabled on the resource), and
    # enable the Java agent for dependency tracking (Service Bus calls, end-to-end transactions).
    APPLICATIONINSIGHTS_AUTHENTICATION_STRING = "Authorization=AAD;ClientId=${data.azurerm_user_assigned_identity.func.client_id}"
    APPLICATIONINSIGHTS_ENABLE_AGENT          = "true"
  }

  lifecycle {
    # Set by 'azure-functions:deploy' and points to the deployed code package. Without this, every apply
    # that changes app_settings removes it and the host runs with no functions (messages stay in the queue).
    ignore_changes = [app_settings["WEBSITE_RUN_FROM_PACKAGE"]]
  }

  # Give the role assignments a head start before the runtime first touches storage and the queue.
  depends_on = [
    azurerm_role_assignment.func_storage_blob_owner,
    azurerm_role_assignment.func_sb_receiver,
  ]
}
