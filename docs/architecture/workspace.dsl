workspace "HMPPS Prisoner Finance (PF)" "All services, systems and components that make up The HMPPS Prisoner Finance Service (PF)" {

    !identifiers hierarchical
    !impliedRelationships true

    !adrs ./adrs

    # !plugin com.structurizr.dsl.plugin.documentation.Mermaid
    # !plugin com.structurizr.dsl.plugin.documentation.PlantUML

    model {
        archetypes {
            api = container {
                technology "Kotlin"
                tag "API"
            }
            springBootAPI = api {
                technology "Spring Boot"
            }
            frontend = container {
                technology "Typescript"
                tag "UI"
            }
            expressFrontend = container {
                technology "ExpressJS"
            }
            datastore = container {
                technology "PostgreSQL"
                tag "Datastore"
            }

            externalSystem = softwareSystem {
                tag "External System"
            }
            legacySystem = softwareSystem {
                tag "Legacy System"
            }

            sync = -> {
                tags "Synchronous"
            }
            https = --sync-> {
                technology "HTTPS"
            }
        }

        users = group "Users" {
            prison = person "Prison staff"
            cashier = person "Prison cashier"
            FBP = person "Finance business partner"
            accountant = person "Accountant"
            prisoner = person "Prisoner"
            FandF = person "Friends and family"
        }

        DPS = group "Digital Prison Services (DPS)" {
            hmpps-auth = externalSystem "HMPPS Auth service"
            hmpps-audit = externalSystem "HMPPS Audit service"
            prisonerProfile = externalSystem "Prisoner profile service"
            prisonerSearch = externalSystem "Prisoner search service"
            activities = externalSystem "Activities service"
            adjudications = externalSystem "Adjudications service"
            integrationAPI = externalSystem "External integration API"
        }

        prisonServices = group "Prison services" {
            launchpad = externalSystem "Launchpad"
            SMTP = externalSystem "Send money to prisoners service"

            domainEvents = externalSystem "Prison domain events" "An AWS SQS queue" {
                tags "Queue"
            }
        }

        PF = softwareSystem "Prisoner finance service" {
          generic = expressFrontend "Prisoner Finance UIs"
          specialised = expressFrontend "Specialised task UIs"
          payments = springBootAPI "Payments API" {
            !docs ./docs/purchasing-processes.md

            credit = component "Credit endpoint"
            debit = component "Debit endpoint"
          }
          accounts = springBootAPI "Accounts API" {
            accounts = component "Accounts endpoint"
            transactions = component "Transactions endpoint"
          }
          sync = springBootAPI "Sync service" "A service to allow NOMIS to sync with Prisoner Finance"  {
            !docs ./docs/sync-processes.md
          }
          GL = datastore "General ledger DB"
        }

        external = group "External vendors" {
            BT = externalSystem "BT PIN phone service"
            DHL = externalSystem "DHL canteen service"
            unilink = externalSystem "Unilink Custodial Management System"
        }

        legacy = group "Legacy systems" {
            NOMIS = legacySystem "NOMIS" "Prison Management System" {
                application = container "NOMIS Application" {
                    technology "Oracle Forms"
                }

                nomis-db = datastore "NOMIS Database" "Data store for prison management. Includes a full history and associated information related to the management of people in prison" {
                    technology "Oracle Database"
                }
            }

            prison-api = legacySystem "Prison API"

            NOMIS.application -> NOMIS.nomis-db "Reads from"
            NOMIS.application -> NOMIS.nomis-db "Writes to"
            prison-api -> NOMIS.nomis-db "Reads from"
            prison-api -> NOMIS.nomis-db "Writes to"
        }

        cabinet = group "Cabinet office" {
            SOP = externalSystem "Single Operating Platform (SOP)"
        }

        GDS = group "Government Digital Service (GDS)" {
            govPay = externalSystem "GOV UK Pay"
        }

        bank = group "Bank accounts" {
            hmppsGeneral = externalSystem "HMPPS general"
            prisonerTrustFunds = externalSystem "Prisoner trust funds"
        }

        prison --https-> PF.generic "Uses"
        prison --https-> PF.specialised "Uses"
        prison --https-> prisonerProfile "Uses"
        prison --https-> prisonerSearch "Uses"
        prison --https-> activities "Uses"
        prison --https-> SMTP "Uses"
        prison --https-> SOP "Uses"
        prison --https-> NOMIS.application "Uses"
        prison --https-> unilink "Uses"

        cashier --https-> SOP "Uses"
        cashier --https-> PF.generic "Uses"
        cashier --https-> PF.specialised "Uses"

        prisoner --https-> launchpad "Uses"
        prisoner --https-> unilink "Uses"

        FBP --https-> SOP "Uses"

        accountant --https-> SOP "Uses"

        FandF --https-> SMTP "Uses"
        FandF --https-> govPay "Uses"

        launchpad --https-> PF.accounts.accounts "Reads from"
        launchpad --https-> PF.payments.debit "Writes to"
        launchpad --https-> PF.accounts.transactions "Reads from"

        activities --https-> PF.payments.credit "Writes to"

        adjudications --https-> PF.payments.debit "Writes to"

        SMTP --https-> prison-api "Writes to"
        SMTP --https-> prison-api "Reads from"
        SMTP --https-> PF.payments.credit "Writes to"
        SMTP --https-> PF.payments.debit "Writes to"
        SMTP --https-> PF.accounts "Reads from"

        integrationAPI --https-> PF.accounts.accounts "Reads from"
        integrationAPI --https-> PF.accounts.transactions "Reads from"
        integrationAPI --https-> PF.payments.credit "Writes to"
        integrationAPI --https-> PF.payments.debit "Writes to"

        prisonerProfile --https-> PF.accounts.accounts "Reads from"
        prisonerProfile --https-> PF.accounts.transactions "Reads from"

        PF.generic --https-> hmpps-auth "Reads from"
        PF.generic --https-> hmpps-audit "Writes to"
        PF.generic --https-> PF.accounts.accounts "Reads from"
        PF.generic --https-> PF.accounts.transactions "Reads from"
        PF.generic --https-> PF.payments.debit "Writes to"
        PF.generic --https-> PF.payments.credit "Writes to"

        PF.specialised --https-> hmpps-auth "Reads from"
        PF.specialised --https-> hmpps-audit "Writes to"
        PF.specialised --https-> PF.accounts.accounts "Reads from"
        PF.specialised --https-> PF.payments.debit "Writes to"
        PF.specialised --https-> PF.payments.credit "Writes to"

        PF.payments.credit --https-> hmpps-auth "Reads from"
        PF.payments.credit --https-> hmpps-audit "Writes to"
        PF.payments.credit --https-> PF.gl "Writes to"
        PF.payments.debit --https-> hmpps-auth "Reads from"
        PF.payments.debit --https-> hmpps-audit "Writes to"
        PF.payments.debit --https-> PF.gl "Writes to"

        PF.accounts.accounts --https-> hmpps-auth "Reads from"
        PF.accounts.accounts --https-> hmpps-audit "Writes to"
        PF.accounts.accounts --https-> PF.gl "Reads from"
        PF.accounts.accounts --https-> prisonerSearch "Searches"

        PF.accounts.transactions --https-> hmpps-auth "Reads from"
        PF.accounts.transactions --https-> hmpps-audit "Writes to"
        PF.accounts.transactions --https-> PF.gl "Reads from"
        PF.accounts.transactions --https-> prisonerSearch "Searches"

        PF.sync --https-> PF.GL "Reads from"
        PF.sync --https-> PF.GL "Writes to"

        PF.GL -> hmppsGeneral "Instructs"
        PF.GL -> prisonerTrustFunds "Instructs"
        PF.GL -> SOP "Instructs"

        GOVPay -> prisonerTrustFunds "Moves money into"

        SOP -> hmppsGeneral "Instructs"
        SOP -> prisonerTrustFunds "Instructs"

        BT --https-> integrationAPI "Writes to"
        BT --https-> integrationAPI "Reads from"

        DHL --https-> integrationAPI "Writes to"
        DHL --https-> integrationAPI "Reads from"

        unilink --https-> integrationAPI "Writes to"
        unilink --https-> integrationAPI "Reads from"

        hmpps-auth --https-> NOMIS.nomis-db "Reads from"

        # Sync with NOMIS
        NOMIS.application --https-> PF.sync "Writes to"
        NOMIS.application --https-> domainEvents "Listens to"
        PF.sync --https-> domainEvents "Pushes to"

        dev = deploymentEnvironment "DEV" {

            MOJ = deploymentNode "MoJ Cloud Platform" {
                tags "Amazon Web Services - Cloud"

                cloud = deploymentNode "EU-West-2 (London)" {
                    tags "Amazon Web Services - Region"

                    route53 = infrastructureNode "Route 53" {
                        tags "Amazon Web Services - Route 53"
                    }
                    elb = infrastructureNode "Elastic Load Balancer" {
                        tags "Amazon Web Services - Elastic Load Balancing"
                    }
                    domainEvents = infrastructureNode "Domain events" {
                        tags "Amazon Web Services - Simple Notification Service SNS"
                    }

                    route53 --https-> elb "Forwards requests to"

                    k8 = deploymentNode "Kubernetes" {
                        tags "Amazon Web Services - Kubernetes Service"

                        genericWebApplicationInstance = containerInstance PF.generic
                        specialisedWebApplicationInstance = containerInstance PF.specialised
                        accountsApiApplicationInstance = containerInstance PF.accounts
                        paymentsApiApplicationInstance = containerInstance PF.payments
                        syncApplicationInstance = containerInstance PF.sync
                    }

                    elb --https-> k8.genericWebApplicationInstance "Forwards requests to"
                    elb --https-> k8.specialisedWebApplicationInstance "Forwards requests to"
                    elb --https-> k8.accountsApiApplicationInstance "Forwards requests to"
                    elb --https-> k8.paymentsApiApplicationInstance "Forwards requests to"

                    k8.syncApplicationInstance --https-> domainEvents "Publishes to"
                    k8.syncApplicationInstance --https-> domainEvents "Listens to"

                    rds = deploymentNode "Amazon RDS" {
                        tags "Amazon Web Services - RDS PostgreSQL instance"

                        generalLedger = containerInstance PF.gl
                    }
                }
            }
        }

    }

    views {
        properties {
            "mermaid.url" "http://localhost:8888"
            "mermaid.format" "svg"
            "plantuml.url" "http://localhost:8888"
            "plantuml.format" "svg"
        }

        /*
        systemLandscape PF {
            include *
            autolayout tb
        }

        container PF {
            include *
            autolayout lr
        }
        */

        styles {

            element "Software System" {
                background #1168bd
                color #ffffff
            }

            element "Legacy System" {
                background #cccccc
                color #000000
            }

            element "External System" {
                background #3598EE
                color #000000
            }

            element "Person" {
                shape person
                background #08427b
                color #ffffff
            }

            element "Datastore" {
                shape cylinder
            }

            element "Lambda" {
                shape circle
                background #FFAC33
            }

            theme default
            themes https://static.structurizr.com/themes/amazon-web-services-2020.04.30/theme.json
        }
    }
}
