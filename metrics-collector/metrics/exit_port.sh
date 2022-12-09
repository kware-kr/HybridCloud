NAME=$1
ECHO=$2

# sudo netstat -tnlp | grep kubectl

if [[ -f ".$NAME" ]]; then
  PID=$(cat .$NAME)

  if [[ ! -z ".$NAME" ]]; then
    echo "kill $PID"
    kill -15 $PID
    kill -9 $PID
  fi

  echo "remove .$NAME"
  rm .$NAME
  echo $2
fi