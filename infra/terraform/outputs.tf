output "key_vault_uri" {
  value = azurerm_key_vault.this.vault_uri
}

output "api_url" {
  value = "https://${azurerm_linux_web_app.api.default_hostname}"
}

output "api_name" {
  value = azurerm_linux_web_app.api.name
}

