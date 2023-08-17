
all: sub-clean sub-install sub-test

sub-clean:
	lein sub clean

sub-release:
	lein sub release

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

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md

pg-logs:
	tail -f '/Users/ivan/Library/Application Support/Postgres/var-14/postgresql.log'

psql-ssl:
	PGSSLMODE=verify-full PGSSLROOTCERT=certs/server.crt psql -h localhost -p 35432 -U test test

docs-trigger:
	curl -X POST -d com.github.igrishaev/pg-client -d version=0.1.1 https://cljdoc.org/api/request-build2
