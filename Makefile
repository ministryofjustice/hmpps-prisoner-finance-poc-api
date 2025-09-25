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

.PHONY: authenticate-docker build-dev test serve publish unit-test build lint
