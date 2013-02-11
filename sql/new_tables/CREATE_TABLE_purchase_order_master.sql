-- Table: purchase_order_master

-- DROP TABLE purchase_order_master;

CREATE TABLE purchase_order_master
(
  order_id integer NOT NULL,
  master integer NOT NULL,
  CONSTRAINT purchase_order_master_pkey PRIMARY KEY (order_id),
  CONSTRAINT purchase_order_master_order_id_fkey FOREIGN KEY (order_id)
      REFERENCES purchase_order (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE purchase_order_master
  OWNER TO jbilling;
