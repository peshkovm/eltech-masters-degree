package com.github.denpeshkov;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

public class GenerateDataset {

  static final String inDatasetPath =
      "C:/Users/Денис/Downloads/PAMAP2_Dataset/PAMAP2_Dataset/Protocol";

  public static void main(String[] args) throws IOException {
    GenerateDataset.orig();
  }

  private static void orig() throws IOException {
    final String outDatasetPath = "pamap2/src/main/resources/pamap2.csv";
    AtomicInteger i = new AtomicInteger();
    AtomicInteger linesCount = new AtomicInteger();

    Files.deleteIfExists(Paths.get(outDatasetPath));
    Files.newDirectoryStream(Paths.get(inDatasetPath))
        .forEach(
            dir -> {
              int[] arr = new int[25];
              if (i.getAndIncrement() >= 0) {
                try {
                  if (!Files.exists(Paths.get(outDatasetPath))) {
                    Files.createFile(Paths.get(outDatasetPath));
                  }

                  System.out.println(dir);

                  Files.lines(dir)
                      .peek(
                          line -> {
                            try {
                              final int id = Integer.parseInt(line.split(" ")[1]);
                              if (arr[id] <= 2000) {
                                arr[id]++;
                                Files.write(
                                    Paths.get(outDatasetPath),
                                    (line + "\n").getBytes(),
                                    StandardOpenOption.APPEND);
                                linesCount.getAndIncrement();
                              }

                            } catch (IOException e) {
                              e.printStackTrace();
                            }
                          })
                      .forEach(line -> {});
                } catch (IOException e) {
                  e.printStackTrace();
                }
              }
            });

    System.out.println("linesCount = " + linesCount);
  }
}
