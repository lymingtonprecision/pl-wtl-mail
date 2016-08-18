# LPE Production Line Fortnightly Work To List Mailer

Extracts the prioritized work to lists for the current fortnight from
IFS and emails them to the Production Line Team Leaders and Production
Supervisors.

## Running

Simply execute the `.jar` file:

    java -jar path/to/pl-wtl-mail.jar

Note, though, that you’ll need to provide the database username and
password under which to run. This can be done in a number of ways:

* As `ENV`ironment variables: `DATABASE_USER` and `DATABASE_PASSWORD`.
* In a configuration file.

  A minimal configuration file containing just these options would look
like:

      {:database {:user "dortiz" :password "secret"}}

  … and should be saved as a `.edn` file in a location of your choosing.
Finally, you should specify this configuration when running the program:

      java -jar path/to/pl-wtl-mail.jar -c path/to/config.edn

* On the command line:

      java -jar path/to/pl-wtl-mail.jar -u awarren -p secret

### Command Line Options

The following command line options are supported:

* `-c` (`--config`) the path to the configuration file to use.
* `-u` (`--user`) database user account.
* `-p` (`--password`) database account password.
* `--dry-run` will just print the emails to the console, without sending them.
* `-r` (`--redirect`) an email address to deliver all generated emails to.

## User Account Setup

Must be run under a user account with the following permissions:

```sql
create user plwtlmailer identified by <password>;
grant create session to plwtlmailer;

-- views
grant select on ifsapp.shop_ord to plwtlmailer;
grant select on ifsapp.shop_order_operation to plwtlmailer;
grant select on ifsapp.work_center to plwtlmailer;
grant select on ifsapp.production_line to plwtlmailer;
grant select on ifsapp.inventory_part to plwtlmailer;
grant select on ifsapp.technical_object_reference to plwtlmailer;
grant select on ifsapp.technical_spec_numeric to plwtlmailer;
grant select on ifsapp.technical_spec_alphanum to plwtlmailer;
-- ials
grant select on ifsinfo.inv_part_cust_part_no to plwtlmailer;
-- packages
grant execute on ifsapp.client_sys to plwtlmailer;
grant execute on ifsapp.lpe_routed_op_util_api to plwtlmailer;
grant execute on ifsapp.lpe_shop_ord_util_api to plwtlmailer;
grant execute on ifsapp.lpe_mail_api to plwtlmailer;
```

## License

Copyright © 2016 Lymington Precision Engineers Co. Ltd.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
