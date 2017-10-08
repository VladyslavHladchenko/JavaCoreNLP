import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

class NLPtools {

    void makeTrainingfile(String in, String out){
        Scanner sc = new Scanner(in);

        try {
            PrintWriter pw = new PrintWriter(out,"UTF-8");
            //String seq = "";
            //String prevtype ="";

            while(sc.hasNextLine()){
                String line = sc.nextLine();

                if(line.equals("")) {
                    pw.println();
                    //seq = "";
                }
                else if(line.contains("DOCSTART")) {
                    continue;
                }
                else if(line.endsWith("I-ORG")) {
                    pw.println(line.substring(0, line.indexOf(' ')) + "\tORGANIZATION");
                }
                else if(line.endsWith("I-PER")){
                    pw.println(line.substring(0,line.indexOf(' ')) + "\tPERSON");
                }
                else{
                    pw.println(line.substring(0,line.indexOf(' ')) + "\tO");
                }

            }

        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    void makeTestfile(String in, String out){
        try {
            List<Path> paths = Files.find(Paths.get(in),2,(path, attr) ->
                    String.valueOf(path).endsWith(".ann")).collect(Collectors.toList());

            PrintWriter wr = new PrintWriter(out, "UTF-8");

            for(Path path : paths){
                Scanner text = new Scanner(path.toAbsolutePath().toString().substring(0,path.toAbsolutePath().toString().lastIndexOf('.')) + ".txt");
                Scanner ann = new Scanner(path.toFile());

                

            }





        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}