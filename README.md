# Clojure and Finance

This is an exploration of how to use Clojure in the context of financial
applications.

## Part 1: Building a bank on your laptop

- [REPL worksheet](src/kth_clj_finance/banking.clj): you can walk through this,
  evaluating expressions in order, and at the end you will have a bank! This is
  optimised for focusing on one expression at a time, so there are lots of
  line breaks between expressions.
- [REPL session](src/kth_clj_finance/local.clj): this is basically the same as
  the worksheet, except the output of each expression is also present. 

## Part 2: Building a bank in the cloud

- [REPL worksheet](src/kth_clj_finance/clouds.clj): you can walk through this,
  evaluating expressions in order, and at the end you will have a bank in AWS!
  This is optimised for focusing on one expression at a time, so there are lots
  of line breaks between expressions.
- [REPL session](src/kth_clj_finance/aws.clj): this is basically the same as
  the worksheet, except the output of each expression is also present. 

Note that you will need an AWS account here, and will have had to create a
profile using the credentials file method as documented in the AWS
[Configuration and credential file
settings](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)
documentation.

### Building the API Gateway lambda handler

``` sh
make clean apigw
```
