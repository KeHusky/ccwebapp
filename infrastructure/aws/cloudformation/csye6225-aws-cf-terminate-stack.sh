read -p "profile: " PROFILE
export AWS_PROFILE=$PROFILE
read -p "stack name: " s_name

aws cloudformation delete-stack --stack-name $s_name
aws cloudformation wait stack-delete-complete --stack-name $s_name

echo "$Stack_Name Stack is deleted successfully"
