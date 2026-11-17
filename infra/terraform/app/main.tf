# App stack: resources that are DESTROYED nightly (cost).
# Reads the foundation resources by name, so it does not depend on where the foundation state lives.
# Apply foundation/ first.

data "azurerm_resource_group" "this" {
  name = var.resource_group_name
}

data "azurerm_user_assigned_identity" "api" {
  name                = "id-${var.workload}-${var.environment}-${var.region_abbr}"
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
  }
}
