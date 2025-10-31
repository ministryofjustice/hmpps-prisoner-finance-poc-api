# HMPPS Prisoner Finance API

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-prisoner-finance-poc-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-prisoner-finance-poc-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-prisoner-finance-poc-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://prisoner-finance-poc-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

## Contents
- [About this project](#about-this-project)
- [Project set up](#project-set-up)
- [Running the application locally](#running-the-application-locally)
  - [Running the application in intellij](#running-the-application-in-intellij)
- [Architecture](#Architecture)

## About this project

An API used by the NOMIS application to sync with the Prisoner Finance Ledger.

It is built using [Spring Boot](https://spring.io/projects/spring-boot/) and [Kotlin](https://kotlinlang.org/) as well as the following technologies for its infrastructure:

- [AWS](https://aws.amazon.com/) - Services utilise AWS features through Cloud Platform.
- [Cloud Platform](https://user-guide.cloud-platform.service.justice.gov.uk/#cloud-platform-user-guide) - Ministry of Justice's (MOJ) cloud hosting platform built on top of AWS which offers numerous tools such as logging, monitoring and alerting for our services.
- [Docker](https://www.docker.com/) - The API is built into docker images which are deployed to our containers.
- [Kubernetes](https://kubernetes.io/docs/home/) - Creates 'pods' to host our environment. Manages auto-scaling, load balancing and networking to our application.

## Project set up

Enable pre-commit hooks for formatting and linting code with the following command;

```bash
./gradlew addKtlintFormatGitPreCommitHook addKtlintCheckGitPreCommitHook
```

## Running the application locally

The application comes with a `local` spring profile that includes default settings for running locally.

There is also a `docker-compose.yml` that can be used to run a local instance in docker and also an
instance of HMPPS Auth.

```bash
make serve
```

will build the application and run it and HMPPS Auth within a local docker instance.

### Running the application in Intellij

```bash
make serve-environment
```

will just start a docker instance of HMPPS Auth with a PostgreSQL database. The application should then be started with 
a `local` active profile in Intellij.

```bash
make serve-clean-environment
```

will also reset the database

## Architecture

For details of the current proposed architecture [view our C4 documentation](./docs/architecture)
