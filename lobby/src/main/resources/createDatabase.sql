DROP TABLE if exists `comps`;
CREATE TABLE `comps`
(
  `ID`           int(11)     NOT NULL AUTO_INCREMENT,
  `Theme`        varchar(255) DEFAULT NULL,
  `State`        varchar(30) NOT NULL,
  `StartDate`    datetime     DEFAULT NULL,
  `EndDate`      datetime     DEFAULT NULL,
  `VoteEnd`      datetime     DEFAULT NULL,
  `VoteType`     varchar(30) NOT NULL,
  `MaxEntrants`  int(11)     NOT NULL,
  `FirstPrize`   varchar(255) DEFAULT NULL,
  `SecondPrize`  varchar(255) DEFAULT NULL,
  `DefaultPrize` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 19
  DEFAULT CHARSET = latin1;
DROP TABLE if exists `criteria`;
CREATE TABLE `criteria`
(
  `CriteriaID`  int(11)     NOT NULL AUTO_INCREMENT,
  `CompID`      int(11)     NOT NULL,
  `Name`        varchar(30) NOT NULL,
  `Description` varchar(255) DEFAULT NULL,
  `Type`        varchar(30)  DEFAULT NULL,
  `Data`        varchar(255) DEFAULT NULL,
  PRIMARY KEY (`CriteriaID`),
  KEY `CompID` (`CompID`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 37
  DEFAULT CHARSET = latin1;
DROP TABLE if exists `servers`;
CREATE TABLE `servers`
(
  `ServerID` varchar(30) NOT NULL,
  `CompID`   int(11) DEFAULT NULL,
  PRIMARY KEY (`ServerID`),
  KEY `CompID` (`CompID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;
DROP TABLE if exists `results`;
CREATE TABLE `results`
(
  `CompID`  int(11)  NOT NULL,
  `UUID`    char(36) NOT NULL,
  `Name`    varchar(20)  DEFAULT NULL,
  `Rank`    int(11)      DEFAULT NULL,
  `PlotID`  varchar(10)  DEFAULT NULL,
  `Prize`   varchar(255) DEFAULT NULL,
  `Claimed` tinyint(1)   DEFAULT NULL,
  PRIMARY KEY (`CompID`, `UUID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;
DROP TABLE if exists `votes`;
CREATE TABLE `votes`
(
  `CompID`    int(11)     NOT NULL,
  `UUID`      char(36)    NOT NULL,
  `PlotID`    varchar(10) NOT NULL,
  `PlotOwner` char(36)    NOT NULL,
  `Vote`      int(11)     NOT NULL,
  PRIMARY KEY (`CompID`, `UUID`, `PlotID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;
DROP TABLE if exists `whitelist`;
CREATE TABLE `whitelist`
(
  `UUID` char(32) NOT NULL,
  PRIMARY KEY (`UUID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;
