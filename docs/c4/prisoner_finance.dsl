workspace {

    !identifiers hierarchical
    !impliedRelationships true

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

            sync = -> {
                tags "Synchronous"
            }
            https = --sync-> {
                technology "HTTPS"
            }
        }

        users = group "Users" {
            prison = person "Prison staff"
            FBP = person "Finance business partner"
            accountant = person "Accountant"
            prisoner = person "Prisoner"
            FandF = person "Friends and family"
        }

        DPS = group "Digital Prison Services (DPS)" {
            launchpad = softwareSystem "Launchpad"
            prisonerProfile = softwareSystem "Prisoner profile service"
            prisonerSearch = softwareSystem "Prisoner search service"
            activities = softwareSystem "Activities service"
            adjudications = softwareSystem "Adjudications service"
            SMTP = softwareSystem "Send money to prisoners service"
            integrationAPI = softwareSystem "External integration API"
        }

        PF = softwareSystem "Prisoner finance service" {
          generic = expressFrontend "Prisoner Finance UIs"
          specialised = expressFrontend "Specialised task UIs"
          payments = springBootAPI "Payments API" {
            credit = component "Credit endpoint"
            debit = component "Debit endpoint"
          }
          accounts = springBootAPI "Accounts API" {
            accounts = component "Accounts endpoint"
            transactions = component "Transactions endpoint"
          }
          gl = datastore "General ledger DB"
        }

        external = group "External vendors" {
            bt = softwareSystem "BT PIN phone service"
            dhl = softwareSystem "DHL canteen service"
        }

        legacy = group "Legacy systems" {
            NOMIS = softwareSystem "NOMIS" {
                tags "Legacy System"

                prisonApi = container "Prison API"
                database  = container "NOMIS DB"

                prisonApi -> database "reads / writes"
            }
        }

        cabinet = group "Cabinet office" {
            SOP = softwareSystem "Single Operating Platform (SOP)"
        }

        GDS = group "Government Digital Service (GDS)" {
            govPay = softwareSystem "GOV UK Pay"
        }

        bank = group "Bank accounts" {
            hmppsGeneral = softwareSystem "HMPPS general"
            prisonerTrustFunds = softwareSystem "Prisoner trust funds"
        }

        prison --https-> PF.generic "Uses"
        prison --https-> PF.specialised "Uses"
        prison --https-> prisonerProfile "Uses"
        prison --https-> prisonerSearch "Uses"
        prison --https-> activities "Uses"
        prison --https-> SMTP "Uses"
        prison --https-> SOP "Uses"
        prison --https-> NOMIS "Uses"

        prisoner --https-> launchpad "Uses"

        FBP --https-> SOP "Uses"

        accountant --https-> SOP "Uses"

        FandF --https-> SMTP "Uses"
        FandF --https-> govPay "Uses"

        launchpad --https-> PF.payments.debit "Writes to"
        launchpad --https-> PF.accounts.accounts "Reads from"
        launchpad --https-> PF.accounts.transactions "Reads from"

        activities --https-> PF.payments.credit "Writes to"

        adjudications --https-> PF.payments.debit "Writes to"

        SMTP --https-> PF.payments.credit "Writes to"
        SMTP --https-> PF.payments.debit "Writes to"
        SMTP --https-> PF.accounts "Reads from"

        integrationAPI --https-> PF.accounts.accounts "Reads from"
        integrationAPI --https-> PF.accounts.transactions "Reads from"
        integrationAPI --https-> PF.payments.credit "Writes to"
        integrationAPI --https-> PF.payments.debit "Writes to"

        prisonerProfile --https-> PF.accounts.accounts "Reads from"
        prisonerProfile --https-> PF.accounts.transactions "Reads from"

        PF.generic --https-> PF.accounts.accounts "Reads from"
        PF.generic --https-> PF.accounts.transactions "Reads from"
        PF.generic --https-> PF.payments.debit "Writes to"
        PF.generic --https-> PF.payments.credit "Writes to"

        PF.specialised --https-> PF.accounts.accounts "Reads from"
        PF.specialised --https-> PF.payments.debit "Writes to"
        PF.specialised --https-> PF.payments.credit "Writes to"

        PF.payments.credit --https-> PF.gl "Writes to"
        PF.payments.debit --https-> PF.gl "Writes to"

        PF.accounts.accounts --https-> PF.gl "Reads from"
        PF.accounts.accounts --https-> prisonerSearch "Searches"

        PF.accounts.transactions --https-> PF.gl "Reads from"
        PF.accounts.transactions --https-> prisonerSearch "Searches"

        PF.GL -> hmppsGeneral "Instructs"
        PF.GL -> prisonerTrustFunds "Instructs"
        PF.GL -> SOP "Instructs"

        GOVPay -> prisonerTrustFunds "Moves money into"

        SOP -> hmppsGeneral "Instructs"
        SOP -> prisonerTrustFunds "Instructs"

        BT --https-> PF.payments "Writes to"
        BT --https-> PF.accounts "Reads from"

        DHL --https-> PF.payments "Writes to"
        DHL --https-> PF.accounts "Reads from"



        dev = deploymentEnvironment "DEV" {

            MOJ = deploymentNode "Amazon Web Services - MoJ Cloud Platform" {
                tags "Amazon Web Services - Cloud"

                cloud = deploymentNode "EU-West-2 London" {
                    tags "Amazon Web Services - Region"

                    route53 = infrastructureNode "Route 53"
                    elb = infrastructureNode "Elastic Load Balancer"

                    route53 --https-> elb "Forwards requests to"

                    k8 = deploymentNode "Kubernetes" {
                        tags "Amazon Web Services - Kubernetes Service"

                        genericWebApplicationInstance = containerInstance PF.generic
                        specialisedWebApplicationInstance = containerInstance PF.specialised
                        accountsApiApplicationInstance = containerInstance PF.accounts
                        paymentsApiApplicationInstance = containerInstance PF.payments
                    }

                    elb --https-> k8.genericWebApplicationInstance "Forwards requests to"
                    elb --https-> k8.specialisedWebApplicationInstance "Forwards requests to"
                    elb --https-> k8.accountsApiApplicationInstance "Forwards requests to"
                    elb --https-> k8.paymentsApiApplicationInstance "Forwards requests to"

                    rds = deploymentNode "Amazon RDS" {
                        postgreSQL = deploymentNode "PostgreSQL" {
                            generalLedger = containerInstance PF.gl
                        }
                    }

                    #k8.genericWebApplicationInstance --https-> rds.postgreSQL.generalLedger "Forwards requests to"
                    #k8.specialisedWebApplicationInstance --https-> rds.postgreSQL.generalLedger "Forwards requests to"
                    #k8.paymentsApiApplicationInstance --https-> rds.postgreSQL.generalLedger "Forwards requests to"
                    #k8.accountsApiApplicationInstance --https-> rds.postgreSQL.generalLedger "Forwards requests to"
                }
            }
        }

    }

    views {
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
        }
    }
}
