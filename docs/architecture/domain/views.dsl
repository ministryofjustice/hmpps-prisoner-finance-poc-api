systemLandscape PF {
    include *

    autolayout tb 150 150
}

systemContext PF {
    include *

    autolayout tb 150 150
}

systemContext NOMIS {
    include *

    autolayout lr 150 150
}

container PF {
    include *

    autolayout tb 150 150
}

component PF.generalLedger {
  include *

  autolayout tb 150 150
}

deployment PF "Dev" {
    include *

    autolayout lr 150 150
}