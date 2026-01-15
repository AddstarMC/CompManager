-- Migration: Add plot_entries table to track competition entries
-- This table allows external web applications to query all entries for a competition

CREATE TABLE IF NOT EXISTS `plot_entries`
(
  `CompID`    int(11)     NOT NULL,
  `UUID`      char(36)    NOT NULL,
  `PlotID`    varchar(10) NOT NULL,
  `EntryDate` datetime    NOT NULL,
  PRIMARY KEY (`CompID`, `UUID`),
  KEY `CompID` (`CompID`),
  CONSTRAINT `fk_plot_entries_comp` FOREIGN KEY (`CompID`) REFERENCES `comps` (`ID`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;
