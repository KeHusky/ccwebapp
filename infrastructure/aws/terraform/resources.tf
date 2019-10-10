#providers
provider "aws" {
        region = "${var.region}"
        access_key = "${var.access_key}"
        secret_key = "${var.secret_key}"
}

#resources

resource "aws_vpc" "vpc" {
 # name = "${var.vpc_name}"
  cidr_block = "${var.cidr_vpc}"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags {
    "Environment" = "${var.environment_tag}"
    Name = "${var.vpc_name}"
  }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = "${aws_vpc.vpc.id}"
  tags {
    "Environment" = "${var.environment_tag}"
  }
}

resource "aws_subnet" "subnet_public1" {
  vpc_id = "${aws_vpc.vpc.id}"
  cidr_block = "${var.cidr_subnet1}"
  map_public_ip_on_launch = "true"
  availability_zone = "${var.availability_zone1}"
  tags {
    "Environment" = "${var.environment_tag}"
  }
}

resource "aws_subnet" "subnet_public2" {
  vpc_id = "${aws_vpc.vpc.id}"
  cidr_block = "${var.cidr_subnet2}"
  map_public_ip_on_launch = "true"
  availability_zone = "${var.availability_zone2}"
  tags {
    "Environment" = "${var.environment_tag}"
  }
}

resource "aws_subnet" "subnet_public3" {
  vpc_id = "${aws_vpc.vpc.id}"
  cidr_block = "${var.cidr_subnet3}"
  map_public_ip_on_launch = "true"
  availability_zone = "${var.availability_zone3}"
  tags {
    "Environment" = "${var.environment_tag}"
  }
}

resource "aws_route_table" "rtb_public" {
  vpc_id = "${aws_vpc.vpc.id}"

  route {
      cidr_block = "0.0.0.0/0"
      gateway_id = "${aws_internet_gateway.igw.id}"
  }

  tags {
    "Environment" = "${var.environment_tag}"
  }
}

#resource "aws_route" "r" {
#  route_table_id            = "${aws_route_table.rtb_public.id}"
#  destination_cidr_block    = "0,0,0,0/0"
#  depends_on                = ["aws_route_table.rtb_public"]
#}

resource "aws_route_table_association" "a" {
  subnet_id      = "${aws_subnet.subnet_public1.id}"
  route_table_id = "${aws_route_table.rtb_public.id}"
}

resource "aws_route_table_association" "b" {
  subnet_id      = "${aws_subnet.subnet_public2.id}"
  route_table_id = "${aws_route_table.rtb_public.id}"
}

resource "aws_route_table_association" "c" {
  subnet_id      = "${aws_subnet.subnet_public3.id}"
  route_table_id = "${aws_route_table.rtb_public.id}"
}
