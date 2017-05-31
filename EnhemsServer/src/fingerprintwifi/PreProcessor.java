package fingerprintwifi;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Teo
 * Processing raw wifi fingerprint data to mean values of AP's and
 * variances values. The syntax for input (example):
 *
 *   Fingerprint name: Home test 1
 *   Room name: My room
 *   Date: 2017-06-01 15:25:28
 *   ---------------------
 *   Vujnovic67 (e8:94:f6:fe:83:f2) [-91 ]
 *   ISKONOVAC-D8EE4C (90:ef:68:d8:ee:4d) [-93 -92 ]
 *   dvttt (78:96:82:20:df:f2) [-77 -77 ]
 *   Tech_D0043797 (84:16:f9:01:e5:ae) [-88 -88 ]
 *   Bijeli3 (ec:8a:4c:92:57:10) [-91 ]
 *   ISKONOVAC-D7ED50 (90:ef:68:d7:ed:51) [-90 -90 ]
 *   BnetDvttt (cc:03:fa:e4:a0:8b) [-51 -55 ]
 *   Tech_D0050539 (88:f7:c7:4f:0d:bb) [-77 -62 ]
 *
 * Arguments taken are paths to files to be processed.
 * Data for each room should be separated with at least one newline.
 * If room and fingerprint name are the same they will be processed
 * as one input.
 *
 */
public class PreProcessor {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Provide at least one input file!");
            return;
        }

        //roomValueMap is the value, key is the room name
        HashMap<String,HashMap<String,List<Integer>>> fingerprintMap = new HashMap<>();

        //FILES
        for(int fileNum = 0; fileNum < args.length; fileNum++) {
            String inputFile = args[fileNum];
            try {
                FileInputStream stream = new FileInputStream(
                        System.getProperty("user.dir")
                                + File.separator + "src"
                                + File.separator + "fingerprintwifi"
                                + File.separator + inputFile
                );
                BufferedReader br = new BufferedReader(new InputStreamReader(stream));

                String roomName;
                //ROOMS
                while (true) {
                    //MAC is the key, value is list of RSS values
                    HashMap<String,List<Integer>> roomValuesMap;

                    String fingerprintName = br.readLine().replace("Fingerprint name:", "").trim();
                     roomName = br.readLine().replace("Room name:", "").trim();

                    //if room already exists in data
                    if (fingerprintMap.containsKey(roomName)) {
                        roomValuesMap = fingerprintMap.get(roomName);
                    } else {
                        roomValuesMap = new HashMap<>();
                    }

                    //skip a date
                    br.readLine();
                    //skip a line
                    br.readLine();
                    //Access point line
                    String apLine;
                    //MAC address of AP
                    String mac = "";
                    //RSS values list
                    List<Integer> rssList = new LinkedList<>();


                    //AP'S
                    while ((apLine = br.readLine()) != null) {
                        if (apLine.isEmpty()) {
                            break;
                        }
                        rssList = new LinkedList<>();
                        //get MAC
                        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(apLine);
                        while(m.find()) {
                            mac = m.group(1);
                        }
                        if (mac.isEmpty()) {
                            break;
                        }
                        //get RSS value
                        m = Pattern.compile("\\[([^)]+)\\]").matcher(apLine);
                        String rssValues = "";
                        while(m.find()) {
                            rssValues = m.group(1);
                        }
                        if (rssValues.isEmpty()) {
                            break;
                        }
                        String[] rssStringArr = rssValues.split("\\s+");
                        //RSS
                        for(int rssIndex = 0; rssIndex < rssStringArr.length; rssIndex++) {
                            rssList.add(Integer.parseInt(rssStringArr[rssIndex]));
                        }

                        if (roomValuesMap.containsKey(mac)) {
                            roomValuesMap.get(mac).addAll(rssList);
                        } else {
                            roomValuesMap.put(mac, rssList);
                        }

                    }

                    if (!fingerprintMap.containsKey(roomName)) {
                        fingerprintMap.put(roomName, roomValuesMap);
                    }
                    break;
                }
                System.out.println(fingerprintMap.toString());
                br.close();
            } catch (IOException ex) {
                System.err.println("Problem reading file!");
                ex.printStackTrace();
            }
        }

    }


}