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

        prison = person "Prison user"
        prisoner = person "Prisoner"
        fandf = person "Friends and family"

        DPS = group "Digital Prison Services (DPS)" {
            launchpad = softwareSystem "Launchpad"
            prisonerProfile = softwareSystem "Prisoner profile service"
            prisonerSearch = softwareSystem "Prisoner search service"
            activities = softwareSystem "Activities service"
            SMTP = softwareSystem "Send money to prisoners service"
            integrationAPI = softwareSystem "External integration API"
        }

        PF = softwareSystem "Prisoner finance service" {
          generic = expressFrontend "Prisoner Finance UIs"
          specialised = expressFrontend "Bespoke task UIs"
          payments = springBootAPI "Payments API" {
            credit = component "Credit endpoint"
            debit = component "Debit endpoint"
          }
          accounts = springBootAPI "Accounts API"
          gl = datastore "General ledger DB"
        }

        GDS = group "Government Digital Service (GDS)" {
            govPay = softwareSystem "GOV UK Pay"
        }

        cabinet = group "Cabinet office central services" {
            sop = softwareSystem "Single Operating Platform (SOP)"
        }

        external = group "External vendors" {
            bt = softwareSystem "BT PIN phone service"
            dhl = softwareSystem "DHL canteen service"
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
        prisoner --https-> launchpad "Uses"
        fandf --https-> SMTP "Uses"
        launchpad --https-> PF.payments.debit "Writes to"
        launchpad --https-> PF.accounts "Reads from"
        activities --https-> PF.payments "Writes to"
        SMTP --https-> PF.payments.credit "Writes to"
        SMTP --https-> PF.accounts "Reads from"
        integrationAPI --https-> PF.payments.credit "Writes to"
        integrationAPI --https-> PF.payments.debit "Writes to"
        integrationAPI --https-> PF.accounts "Reads from"
        PF.generic --https-> PF.payments.debit "Writes to"
        PF.generic --https-> PF.payments.credit "Writes to"
        PF.generic --https-> PF.accounts "Reads from"
        PF.specialised --https-> PF.payments "Writes to"
        PF.specialised --https-> PF.accounts "Reads from"
        PF.payments --https-> PF.gl "Writes to"
        PF.accounts --https-> PF.gl "Reads from"
        PF.accounts --https-> prisonerProfile "Reads from"
        PF.accounts --https-> prisonerSearch "Reads from"
        PF.GL -> hmppsGeneral "Instructs"
        PF.GL -> prisonerTrustFunds "Instructs"
        PF.GL -> sop "Instructs"
        govPay -> prisonerTrustFunds "Moves money into"
        sop -> hmppsGeneral "Instructs"
        sop -> prisonerTrustFunds "Instructs"
        bt --https-> PF.payments "Writes to"
        bt --https-> PF.accounts "Reads from"
        dhl --https-> PF.payments "Writes to"
        dhl --https-> PF.accounts "Reads from"

    }

    views {
        systemLandscape PF {
            include *
            autolayout tb
        }

        container PF {
            include *
            autolayout lr
        }
    }

}
