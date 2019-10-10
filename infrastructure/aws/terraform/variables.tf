# Variables
variable "vpc_name" {
  description = "Name for Vpc"
}
#variable "profile"{
#  description = "Switch profile"
#}

variable "region"{
  description = "switch region"
}

variable "access_key"{
  description = "input access key"
}

variable "secret_key"{
  description = "input secret key"
}

variable "cidr_vpc" {
  description = "CIDR block for the VPC"
#  default = "10.0.0.0/16"
}
variable "cidr_subnet1" {
  description = "CIDR block for the subnet"
#  default = "10.0.1.0/24"
}

variable "cidr_subnet2" {
  description = "CIDR block for the subnet"
#  default = "10.0.2.0/24"
}
variable "cidr_subnet3" {
  description = "CIDR block for the subnet"
#  default = "10.0.3.0/24"
}

variable "availability_zone1" {
  description = "availability zone to create subnet"
}

variable "availability_zone2" {
  description = "availability zone to create subnet"
}

variable "availability_zone3" {
  description = "availability zone to create subnet"
}

variable "environment_tag" {
  description = "Environment tag"
  default = "Production"
}
