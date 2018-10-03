-- CREATE KEYSPACE movie WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1};
-- USE movie;

CREATE TABLE IF NOT EXISTS persons (
	id int,
	name text,
	address text,
	age int,
    PRIMARY KEY (id)
);

INSERT INTO persons (id,name,address,age) VALUES (1,'Jurgen Bosch','Sibiu',25);
INSERT INTO persons (id,name,address,age) VALUES (2,'Gheorghe Necula','Bucuresti',32);