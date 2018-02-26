mkdir ~/temp/analyze
cp src/*.java ~/temp/analyze
cp lib/*.jar ~/temp/analyze
cp JackAnalyzer.sh ~/temp/analyze
cp makefile ~/temp/analyze
cd ~/temp/analyze
javac *.java -classpath "./*"
make
zip project10.zip *

