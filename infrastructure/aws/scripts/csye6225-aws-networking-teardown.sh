read -p "profile: " PROFILE
export AWS_PROFILE=$PROFILE
STACK_NAME=new
CIDRBlock_route=0.0.0.0/0
#Get a vpc-Id using the name provided
vpcId=`aws ec2 describe-vpcs --filter "Name=tag:Name,Values=${STACK_NAME}" --query 'Vpcs[*].{id:VpcId}' --output text`
#Get a Internet Gateway Id using the name provided
gatewayId=`aws ec2 describe-internet-gateways --filter "Name=tag:Name,Values=${STACK_NAME}" --query 'InternetGateways[*].{id:InternetGatewayId}' --output text`
#Get a route table Id using the name provided
routeTableId=`aws ec2 describe-route-tables --filter "Name=tag:Name,Values=${STACK_NAME}" --query 'RouteTables[*].{id:RouteTableId}' --output text`
#Get subnet123
subnet1=`aws ec2 describe-subnets --filter "Name=tag:Name,Values=${STACK_NAME}-1" --query 'Subnets[*].{id:SubnetId}' --output text`
echo $subnet1
subnet2=`aws ec2 describe-subnets --filter "Name=tag:Name,Values=${STACK_NAME}-2" --query 'Subnets[*].{id:SubnetId}' --output text`
echo $subnet2
subnet3=`aws ec2 describe-subnets --filter "Name=tag:Name,Values=${STACK_NAME}-3" --query 'Subnets[*].{id:SubnetId}' --output text`
echo $subnet3

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
aws ec2 detach-internet-gateway --internet-gateway-id $gatewayId --vpc-id $vpcId
echo "Detaching the Internet gateway from vpc..."

#Delete the Internet gateway
aws ec2 delete-internet-gateway --internet-gateway-id $gatewayId
echo "Deleting the Internet gateway-> gateway id: "$gatewayId

#Delete the vpc
aws ec2 delete-vpc --vpc-id $vpcId
echo "Deleting the vpc-> vpc id: "$vpcId

echo "Task Completed!"
