variable "resource_group_name" {
  type        = string
  description = "Bestaande resource group (wordt niet door Terraform beheerd)"
}

variable "workload" {
  type    = string
  default = "ivaninterview"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "region_abbr" {
  type    = string
  default = "bec"
}

variable "owner" {
  type = string
}

variable "expiry" {
  type        = string
  description = "Geplande verwijderdatum, YYYY-MM-DD"
}
