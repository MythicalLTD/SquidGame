mvn clean package 

cp target/SquidGame.jar server/plugins/SquidGame.jar

cd server
rm /var/www/SquidGame/server/plugins/SquidGame/messages.yml -rf
rm /var/www/SquidGame/server/plugins/SquidGame/config.yml -rf

java -jar server.jar nogui