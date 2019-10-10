read -p "profile: " PROFILE
export AWS_PROFILE=$PROFILE
if [ -z $PROFILE ]
then 
    echo "cant be null"
    exit
fi
read -p "vpc id: " VPC_ID
if [ -z $VPC_ID ]
then 
    echo "cant be null"
    exit
fi
CIDRBlock_route=0.0.0.0/0
#Get a Internet Gateway Id using the name provided
gatewayId=`aws ec2 describe-internet-gateways --filter "Name=attachment.vpc-id,Values=${VPC_ID}" --query 'InternetGateways[*].{id:InternetGatewayId}' --output text`

#Get subnet123
subnet1=`aws ec2 describe-subnets --filter "Name=vpc-id,Values=${VPC_ID}" --query 'Subnets[0].{id:SubnetId}' --output text`
echo $subnet1
subnet2=`aws ec2 describe-subnets --filter "Name=vpc-id,Values=${VPC_ID}" --query 'Subnets[1].{id:SubnetId}' --output text`
echo $subnet2
subnet3=`aws ec2 describe-subnets --filter "Name=vpc-id,Values=${VPC_ID}" --query 'Subnets[2].{id:SubnetId}' --output text`
echo $subnet3
#Get a route table Id using the name provided
routeTableId=`aws ec2 describe-route-tables --filter "Name=association.subnet-id,Values=${subnet1}" --query 'RouteTables[*].{id:RouteTableId}' --output text`

#Delete subnets
aws ec2 delete-subnet --subnet-id $subnet1
aws ec2 delete-subnet --subnet-id $subnet2
aws ec2 delete-subnet --subnet-id $subnet3
echo "Deleting the subnets..."

#Delete the route
aws ec2 delete-route --route-table-id $routeTableId --destination-cidr-block $CIDRBlock_route
echo "Deleting the route..."

#Delete the route table
aws ec2 delete-route-table --route-table-id $routeTableId
echo "Deleting the route table-> route table id: "$routeTableId

#Detach Internet gateway and vpc
aws ec2 detach-internet-gateway --internet-gateway-id $gatewayId --vpc-id $VPC_ID
echo "Detaching the Internet gateway from vpc..."

#Delete the Internet gateway
aws ec2 delete-internet-gateway --internet-gateway-id $gatewayId
echo "Deleting the Internet gateway-> gateway id: "$gatewayId

#Delete the vpc
aws ec2 delete-vpc --vpc-id $VPC_ID
echo "Deleting the vpc-> vpc id: "$VPC_ID

echo "Task Completed!"
