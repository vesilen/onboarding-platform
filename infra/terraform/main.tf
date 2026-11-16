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
