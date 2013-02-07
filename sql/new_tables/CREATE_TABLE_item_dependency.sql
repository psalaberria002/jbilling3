-- Table: item_dependency

-- DROP TABLE item_dependency;

CREATE TABLE item_dependency
(
  item_id integer NOT NULL,
  child_item_id integer NOT NULL,
  CONSTRAINT item_user_dependency PRIMARY KEY (item_id, child_item_id),
  CONSTRAINT item_dependency_child_item_id_fkey FOREIGN KEY (child_item_id)
      REFERENCES item (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT item_dependency_item_id_fkey FOREIGN KEY (item_id)
      REFERENCES item (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE item_dependency
  OWNER TO jbilling;
