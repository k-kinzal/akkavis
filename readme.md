eval $(minikube docker-env)
sbt docker:publishLocal

kubectl create namespace akkavis
kubectl --namespace akkavis apply -f rbac.yaml

kubectl apply --namespace akkavis -f local.yaml

kubectl port-forward --namespace akkavis svc/akkavis 8080:8080

kubectl delete service akkavis --namespace akka-vis
kubectl delete deployment akkavis --namespace akka-vis