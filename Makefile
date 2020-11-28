.PHONY: apigw
apigw: target/kth-clj-finance.jar

target/kth-clj-finance.jar:
	mkdir -p classes target
	clojure -M:aot
	clojure -M:pack mach.pack.alpha.aws-lambda -e classes target/kth-clj-finance.jar

.PHONY: clean
clean:
	rm -rf classes target
