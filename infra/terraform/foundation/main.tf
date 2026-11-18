# Foundation stack: resources that are KEPT overnight.
# - User-assigned managed identity: same principal_id every day, so role assignments stay valid
# - Key Vault + secret: avoids the soft-delete name lock on recreate (see README)

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

resource "azurerm_user_assigned_identity" "api" {
  name                = "id-${var.workload}-api-${var.environment}-${var.region_abbr}" # id-ivaninterview-api-dev-bec
  location            = data.azurerm_resource_group.this.location
  resource_group_name = data.azurerm_resource_group.this.name
  tags                = local.tags

  lifecycle {
    prevent_destroy = true # remove this line before the final cleanup (expiry)
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

  lifecycle {
    prevent_destroy = true # remove this line before the final cleanup (expiry)
  }
}

# Jijzelf (of wie terraform draait) mag secrets schrijven
resource "azurerm_role_assignment" "deployer_secrets_officer" {
  scope                = azurerm_key_vault.this.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

# The app's managed identity may read secrets, nothing more.
# Lives here (not in app/) because the identity is kept: the assignment survives the nightly destroy.
resource "azurerm_role_assignment" "api_secrets_user" {
  scope                = azurerm_key_vault.this.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_user_assigned_identity.api.principal_id
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

# Identity of the onboarding-processor Function. Separate from the API identity (least privilege:
# the API may only send, the Function may only receive). Kept overnight like the API identity, so the
# role assignments that app/ recreates every morning always point to the same principal_id.
resource "azurerm_user_assigned_identity" "func" {
  name                = "id-${var.workload}-func-${var.environment}-${var.region_abbr}" # id-ivaninterview-func-dev-bec
  location            = data.azurerm_resource_group.this.location
  resource_group_name = data.azurerm_resource_group.this.name
  tags                = local.tags

  lifecycle {
    prevent_destroy = true # remove this line before the final cleanup (expiry)
  }
}

# ---------------------------------------------------------------------------
# Monitoring: Log Analytics + Application Insights
# Kept overnight: free when idle, logs survive the nightly destroy of app/, and the role
# assignments below stay valid because the identities are kept too.
# ---------------------------------------------------------------------------

resource "azurerm_log_analytics_workspace" "this" {
  name                = "log-${var.workload}-${var.environment}-${var.region_abbr}" # log-ivaninterview-dev-bec
  location            = data.azurerm_resource_group.this.location
  resource_group_name = data.azurerm_resource_group.this.name
  sku                 = "PerGB2018" # pay per GB ingested
  retention_in_days   = 30          # included in the price
  daily_quota_gb      = 0.1         # cost cap: collection stops for the rest of the day above 0.1 GB
  tags                = local.tags
}

resource "azurerm_application_insights" "this" {
  name                = "appi-${var.workload}-${var.environment}-${var.region_abbr}" # appi-ivaninterview-dev-bec
  location            = data.azurerm_resource_group.this.location
  resource_group_name = data.azurerm_resource_group.this.name
  workspace_id        = azurerm_log_analytics_workspace.this.id
  application_type    = "java"
  # Only Entra ID (managed identity) may send telemetry; the connection string alone is not enough.
  local_authentication_enabled = false
  tags                         = local.tags
}

# Entra ID ingestion requires this role on the Application Insights resource.
resource "azurerm_role_assignment" "api_appi_publisher" {
  scope                = azurerm_application_insights.this.id
  role_definition_name = "Monitoring Metrics Publisher"
  principal_id         = azurerm_user_assigned_identity.api.principal_id
}

resource "azurerm_role_assignment" "func_appi_publisher" {
  scope                = azurerm_application_insights.this.id
  role_definition_name = "Monitoring Metrics Publisher"
  principal_id         = azurerm_user_assigned_identity.func.principal_id
}
