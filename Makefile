branch=$(shell git rev-parse --abbrev-ref HEAD)

tag=duckietown/amod:$(branch)

build:
	docker build --pull -t $(tag) .
push:
	docker push $(tag)
