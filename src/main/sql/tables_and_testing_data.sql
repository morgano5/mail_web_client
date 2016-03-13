CREATE TABLE `user` (
	`id` int(11) NOT NULL AUTO_INCREMENT,
	`username` varchar(100) NOT NULL,
	`password` char(106) NOT NULL,
	`active` tinyint(4) NOT NULL,
	`version` int(11) NOT NULL,
	PRIMARY KEY (`id`),
	UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

