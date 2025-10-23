build-dev: update-environment
	docker compose build

build:
	docker build -t hmpps-prisoner-finance-poc-api .

update-dependencies:
	./gradlew useLatestVersions

analyse-dependencies:
	./gradlew dependencyCheckAnalyze --info

serve: build-dev
	docker compose up -d --wait

serve-environment:
	docker compose up --scale hmpps-prisoner-finance-poc-api=0 -d --wait

serve-clean-environment: stop-clean
	docker compose up --scale hmpps-prisoner-finance-poc-api=0 -d --wait

update-environment:
	docker compose pull

stop:
	docker compose down

stop-clean:
	docker compose down --remove-orphans --volumes

unit-test:
	./gradlew unitTest

integration-test:
	./gradlew integrationTest --warning-mode all

test: unit-test integration-test

e2e:
	./gradlew integrationTest --warning-mode all

lint:
	./gradlew ktlintCheck

format:
	./gradlew ktlintFormat

check:
	./gradlew check

serve-structurizer:
	docker pull structurizr/lite
	docker run -it --rm -v ./docs/c4:/usr/local/structurizr -p 8080:8080 structurizr/lite

export-c4-mermaid:
	docker pull structurizr/cli:latest
	docker run -it --rm -v ./docs/c4:/usr/local/structurizr structurizr/cli export -w ./workspace.dsl -f mermaid -o ./mermaid

export-c4-plantuml:
	docker pull structurizr/cli:latest
	docker run -it --rm -v ./docs/c4:/usr/local/structurizr structurizr/cli export -w ./workspace.dsl -f plantuml -o ./plantuml

.PHONY: authenticate-docker build-dev test serve publish unit-test build lint
