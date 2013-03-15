-- Table: item_users

-- DROP TABLE item_users;

CREATE TABLE item_users
(
  item_id integer NOT NULL,
  user_id integer NOT NULL,
  users integer,
  CONSTRAINT item_user PRIMARY KEY (item_id, user_id),
  CONSTRAINT item_users_item_id_fkey FOREIGN KEY (item_id)
      REFERENCES item (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT item_users_user_id_fkey FOREIGN KEY (user_id)
      REFERENCES base_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE item_users
  OWNER TO jbilling;
