data "azurerm_client_config" "current" {}

data "azurerm_resource_group" "this" {
  name = var.resource_group_name
}

locals {
  tags = {
    owner   = var.owner
    purpose = "Onboarding platform learning project"
    expiry  = var.expiry
  }
}

resource "azurerm_key_vault" "this" {
  name                       = "kv-${var.workload}-${var.environment}-${var.region_abbr}"
  location                   = data.azurerm_resource_group.this.location
  resource_group_name        = data.azurerm_resource_group.this.name
  tenant_id                  = data.azurerm_client_config.current.tenant_id
  sku_name                   = "standard"
  rbac_authorization_enabled = true
  soft_delete_retention_days = 7
  purge_protection_enabled   = false # anders kan je niet purgen
  tags                       = local.tags
}

# Jijzelf (of wie terraform draait) mag secrets schrijven
resource "azurerm_role_assignment" "deployer_secrets_officer" {
  scope                = azurerm_key_vault.this.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

resource "random_password" "kyc_api_key" {
  length  = 32
  special = false
}

resource "azurerm_key_vault_secret" "kyc_api_key" {
  name         = "kyc-api-key"
  value        = random_password.kyc_api_key.result
  key_vault_id = azurerm_key_vault.this.id

  depends_on = [azurerm_role_assignment.deployer_secrets_officer]
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
    type = "SystemAssigned" # the managed identity
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
    AZURE_KEYVAULT_ENDPOINT = azurerm_key_vault.this.vault_uri
  }
}

# The app's managed identity may read secrets, nothing more
resource "azurerm_role_assignment" "api_secrets_user" {
  scope                = azurerm_key_vault.this.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_linux_web_app.api.identity[0].principal_id
}
