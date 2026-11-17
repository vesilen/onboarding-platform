output "key_vault_uri" {
  value = azurerm_key_vault.this.vault_uri
}

output "identity_client_id" {
  value = azurerm_user_assigned_identity.api.client_id
}
