output "api_url" {
  value = "https://${azurerm_linux_web_app.api.default_hostname}"
}

output "api_name" {
  value = azurerm_linux_web_app.api.name
}

output "servicebus_namespace" {
  value = azurerm_servicebus_namespace.this.name
}

output "servicebus_queue" {
  value = azurerm_servicebus_queue.application_submitted.name
}

output "function_app_name" {
  value = azurerm_linux_function_app.processor.name
}
