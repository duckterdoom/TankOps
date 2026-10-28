    package tankops;

    import com.mongodb.client.MongoClient;
    import com.mongodb.client.MongoClients;
    import com.mongodb.client.MongoDatabase;
    import org.bson.Document;

    public class MongoDatabaseManager {
        private final MongoClient mongoClient; //quản lý kết nối database
        private final MongoDatabase database; //biến database được ctrnh sử dụng

//        public MongoDatabaseManager() { // constructor
//
//            //String connectionString = "mongodb://localhost:27017"; test local
//            String connectionString = "mongodb+srv://admin:0913596688@tankops.cijhhq7.mongodb.net/?compressors=zlib"; //Mongo atlas
//
//            this.mongoClient = MongoClients.create(connectionString);
//            this.database = mongoClient.getDatabase("tankops_db");
//            System.out.println("Đã kết nối thành công tới MongoDB Atlas!");
//        }
    public MongoDatabaseManager() {

            String connectionString =
                    "mongodb+srv://admin:Hautheorganic2005@tankops.cijhhq7.mongodb.net/?compressors=zlib";
//            String connectionString = "mongodb://localhost:27017";

            mongoClient = MongoClients.create(connectionString);

            database = mongoClient.getDatabase("tankops_db");

            // Kiem tra ket noi
            database.runCommand(new Document("ping", 1));

            System.out.println("Ket noi MongoDB thanh cong!");
        }
        public MongoDatabase getDatabase() {
            return database;
        }

        public void close() {
            mongoClient.close();
        }
    }