# getting started

From the root of this repository run the following command to start the structurizr lite web service.

```shell
make serve-structurizer
```

this will create a web service available at `http://localhost:8080` which will allow you to explore the workspace.

## Exporting to Mermaid

Mermaid code can be used in Github Markdown so to export views to mermaid run the following docker command;

```shell
make export-c4-mermaid
```
## Exporting to PlantUML

To export views to PlantUML run the following docker command;

```shell
make export-c4-plantuml
```

# Architecture

```mermaid
C4Container
    title Container Context diagram for Prisoner Finance

    Person(1, "Prison user")

    Enterprise_Boundary(hmpps, "HMPPS") {
        System_Boundary(DPS, "Digital Prison Services (DPS)") {
            System_Ext(4, "Launchpad")
            System_Ext(5, "Prisoner profile service")
            System_Ext(6, "Prisoner search service")
            System_Ext(7, "Activities service")
            System_Ext(8, "Send money to prisoners service")
            System_Ext(9, "External integration API")
        }

        Container_Boundary(PF, "Prisoner finance service") {
            Container(11, "Prisoner Finance UIs")
            Container(12, "Bespoke task UIs")
            Container(13, "Payments API")
            Container(16, "Accounts API")
            ContainerDb(17, "General ledger DB")
        }
    }

    Enterprise_Boundary(cabinet, "Cabinet office central services") {
        System_Ext(19, "Single Operating Platform (SOP)")
    }

    Enterprise_Boundary(external, "External vendors") {
        System_Ext(20, "BT PIN phone service")
        System_Ext(21, "DHL canteen service")
    }

    Enterprise_Boundary(bank, "Bank accounts") {
        System_Ext(22, "HMPPS general")
        System_Ext(23, "Prisoner trust funds")
    }

    Rel(1, 11, "Uses [HTTPS]")
    Rel(1, 12, "Uses [HTTPS]")
    Rel(1, 5, "Uses [HTTPS]")
    Rel(1, 6, "Uses [HTTPS]")
    Rel(1, 7, "Uses [HTTPS]")
    Rel(1, 8, "Uses [HTTPS]")

    Rel(4, 13, "Writes to [HTTPS]")
    Rel(4, 16, "Reads from [HTTPS]")
    Rel(7, 13, "Writes to [HTTPS]")
    Rel(8, 13, "Writes to [HTTPS]")
    Rel(8, 16, "Reads from [HTTPS]")

    Rel(9, 13, "Writes to [HTTPS]")
    Rel(9, 16, "Reads from [HTTPS]")

    Rel(11, 13, "Writes to [HTTPS]")
    Rel(11, 16, "Reads from [HTTPS]")

    Rel(12, 13, "Writes to [HTTPS]")
    Rel(12, 16, "Reads from [HTTPS]")

    Rel(13, 17, "Writes to [HTTPS]")

    Rel(16, 17, "Writes to [HTTPS]")
    Rel(16, 5, "Writes to [HTTPS]")
    Rel(16, 6, "Writes to [HTTPS]")

    Rel(17, 22, "Instructs ADI")
    Rel(17, 23, "Instructs ADI")
    Rel(17, 19, "Instructs ADI")

    Rel(22, 19, "Updates Bank statement")
    Rel(23, 19, "Updates Bank statement")

    Rel(20, 13, "Writes to [HTTPS]")
    Rel(20, 16, "Reads from [HTTPS]")

    Rel(21, 13, "Writes to [HTTPS]")
    Rel(21, 16, "Reads from [HTTPS]")

    UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="2")
```