
all: sub-clean sub-install sub-test

sub-clean:
	lein sub clean

release:
	lein release

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

toc-doc:
	node_modules/.bin/markdown-toc -i doc/010-installation.md
	node_modules/.bin/markdown-toc -i doc/020-client.md
	node_modules/.bin/markdown-toc -i doc/025-notifications.md
	node_modules/.bin/markdown-toc -i doc/030-pool.md
	node_modules/.bin/markdown-toc -i doc/070-arrays.md
	node_modules/.bin/markdown-toc -i doc/080-ssl.md
	node_modules/.bin/markdown-toc -i doc/090-copy.md
	node_modules/.bin/markdown-toc -i doc/100-honey.md

pg-logs:
	tail -f '/Users/ivan/Library/Application Support/Postgres/var-14/postgresql.log'

psql-ssl:
	PGSSLMODE=verify-full PGSSLROOTCERT=certs/server.crt psql -h localhost -p 35432 -U test test

version ?= $(error Please specify the version=... argument)

docs-trigger:
	curl -v -X POST \
		-d project=com.github.igrishaev/pg-client \
		-d version=${version} \
		https://cljdoc.org/api/request-build2

build ?= $(error Please specify the build=... argument)

docs-build-info:
	curl -s -H 'Accept: application/json' https://cljdoc.org/builds/${build} | jq

# https://gist.github.com/achesco/b893fb55b90651cf5f4cc803b78e19fd
certs-generate:
	mkdir -p certs
	cd certs && umask u=rw,go= && openssl req -days 3650 -new -text -nodes -subj '/C=US/ST=Test/L=Test/O=Personal/OU=Personal/emailAddress=test@test.com/CN=localhost' -keyout server.key -out server.csr
	cd certs && umask u=rw,go= && openssl req -days 3650 -x509 -text -in server.csr -key server.key -out server.crt
	cd certs && umask u=rw,go= && cp server.crt root.crt
	cd certs && rm server.csr
	cd certs && umask u=rw,go= && openssl req -days 3650 -new -nodes -subj '/C=US/ST=Test/L=Test/O=Personal/OU=Personal/emailAddress=test@test.com/CN=test' -keyout client.key -out client.csr
	cd certs && umask u=rw,go= && openssl x509 -days 3650 -req  -CAcreateserial -in client.csr -CA root.crt -CAkey server.key -out client.crt
	cd certs && rm client.csr
