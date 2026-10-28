create table payment_processing (
    payment_id UUID not null primary key ,
    client_id varchar(50) not null,
    amount numeric(18,2) not null,
    currency varchar(3) not null,
    status varchar(50) not null
);
