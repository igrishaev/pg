
all: sub-install sub-test

sub-install:
	lein sub install

sub-test:
	lein sub test

docker-up:
	docker-compose up

docker-down:
	docker-compose down --remove-orphans

docker-rm:
	docker-compose rm --force

docker-psql:
	psql --port 35432 --host localhost -U test test
