package com.gerixsoft;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.TreeMap;

public class StellarisSorter {

    public static void main(String[] args) throws Exception {
        System.out.println("Stellaris Sorter v0.1");

        if (args.length != 1)
            throw new IllegalArgumentException("expected file name");

        StellarisSorter obj = new StellarisSorter();
        obj.run(args[0]);
    }

    private void run(String fileName) throws IOException {
        if (!fileName.endsWith(".sav"))
            throw new IllegalArgumentException(fileName);
        Path path = Paths.get(fileName);
        if (!Files.exists(path))
            throw new FileNotFoundException(fileName);

        System.out.print(path);
        System.out.print(' ');

        FileSystem zipFS = FileSystems.newFileSystem(path, null);
        try {
        String gameState = new String(Files.readAllBytes(zipFS.getPath("gamestate")));

        System.out.println("->");

        int country_beginIndex = gameState.indexOf("country={");
        if (country_beginIndex == -1)
            throw new InternalError();
        country_beginIndex += "country={".length();

        int ownedPlanets_beginIndex = gameState.indexOf("owned_planets={", country_beginIndex);
        if (ownedPlanets_beginIndex == -1)
            throw new InternalError();
        ownedPlanets_beginIndex += "owned_planets={".length();
        int ownedPlanets_endIndex = gameState.indexOf('}', ownedPlanets_beginIndex);
        if (ownedPlanets_endIndex == -1)
            throw new InternalError();
        String ownedPlanets = gameState.substring(ownedPlanets_beginIndex, ownedPlanets_endIndex).trim();
        System.out.println("\tfound owned_planets = " + ownedPlanets);

        int planets_beginIndex = gameState.indexOf("planets={");
        if (planets_beginIndex == -1)
            throw new InternalError();
        planets_beginIndex += "planets={".length();

        Map<String, String> sortedPlanets = new TreeMap<String, String>();        
        for (String ownedPlanet : ownedPlanets.split("[ \t]")) {
            int planet_beginIndex = gameState.indexOf("\t" + ownedPlanet + "={", planets_beginIndex);
            if (planet_beginIndex == -1)
                throw new InternalError("not found planet " + ownedPlanet);
            planet_beginIndex += ("\t" + ownedPlanet + "={").length();

            int name_beginIndex = gameState.indexOf("name=\"", planet_beginIndex);
            if (name_beginIndex == -1)
                throw new InternalError();
            name_beginIndex += "name=\"".length();
            int name_endIndex = gameState.indexOf('"', name_beginIndex);
            if (ownedPlanets_endIndex == -1)
                throw new InternalError();
            String name = gameState.substring(name_beginIndex, name_endIndex).trim();

            int planet_endIndex = gameState.indexOf("auto_slots_taken={", name_endIndex);
            if (planet_endIndex == -1)
                throw new InternalError();

            String designation;
            int designation_beginIndex = gameState.indexOf("final_designation=\"", name_endIndex);
            if (designation_beginIndex == -1 || designation_beginIndex > planet_endIndex)
                designation = "";
            else {
                designation_beginIndex += "final_designation=\"".length();
                int designation_endIndex = gameState.indexOf('"', designation_beginIndex);
                if (ownedPlanets_endIndex == -1)
                    throw new InternalError();
                designation = gameState.substring(designation_beginIndex, designation_endIndex).trim();
            }

            System.out.println(String.format("\tfound owned_planet = { id = %s, name = '%s', designation = '%s' }",
                    ownedPlanet, name, designation));

            int z;
            if (designation.equals("col_capital"))
                z = 0;
            else if (designation.startsWith("col_ecu_"))
                z = 1;
            else if (designation.startsWith("col_ring_"))
                z = 2;
            else if (designation.startsWith("col_habitat_"))
                z = 3;
            else if (designation.isEmpty()) // is colonized
                z = 5;
            else {
                designation = "fake_col_planet";
                z = 6; // regular planet
            }
            sortedPlanets.put(String.format("%d\tdesignation='%s', name='%s'", z, designation, name), ownedPlanet);
        }

        System.out.println("sorted planets:");
        for (Map.Entry<String, String> sortedPlanet : sortedPlanets.entrySet())
            System.out.println(String.format("\tid = %s, %s", sortedPlanet.getValue(), sortedPlanet.getKey().substring(sortedPlanet.getKey().indexOf('\t') + "\t".length())));

        String newOwnedPlanets = String.join(" ", sortedPlanets.values());
        System.out.println("result:");
        System.out.println("\t" + newOwnedPlanets);
        
        String newGameState = gameState.substring(0, ownedPlanets_beginIndex) + System.lineSeparator() + "\t\t\t" + newOwnedPlanets + System.lineSeparator() + "\t\t" + gameState.substring(ownedPlanets_endIndex);
        Files.write(zipFS.getPath("gamestate"), newGameState.getBytes(), StandardOpenOption.WRITE);
        } finally {
            zipFS.close();
        }
        System.out.println("done!");
    }
}
