/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.common.command.modules;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.memory.HeapDump;

import okhttp3.MediaType;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class MemoryModule<S> implements CommandModule<S> {
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Override
    public void registerCommands(Consumer<Command<S>> consumer) {
        consumer.accept(Command.<S>builder()
                .aliases("heapdump", "heap")
                .executor((platform, sender, arguments) -> {
                    platform.runAsync(() -> {
                        platform.sendPrefixedMessage("&7Creating a new heap dump, please wait...");

                        HeapDump heapDump;
                        try {
                            heapDump = HeapDump.createNew();
                        } catch (Exception e) {
                            platform.sendPrefixedMessage("&cAn error occurred whilst inspecting the heap.");
                            e.printStackTrace();
                            return;
                        }

                        byte[] output = heapDump.formCompressedDataPayload();
                        try {
                            String key = SparkPlatform.BYTEBIN_CLIENT.postGzippedContent(output, JSON_TYPE);
                            platform.sendPrefixedMessage("&bHeap dump output:");
                            platform.sendLink(SparkPlatform.VIEWER_URL + key);
                        } catch (IOException e) {
                            platform.sendPrefixedMessage("&cAn error occurred whilst uploading the data.");
                            e.printStackTrace();
                        }
                    });
                })
                .build()
        );

        consumer.accept(Command.<S>builder()
                .aliases("memory")
                .executor((platform, sender, arguments) -> {
                    platform.runAsync(() -> {
                        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
                        List<MemoryManagerMXBean> memoryManagerMXBeans = ManagementFactory.getMemoryManagerMXBeans();
                        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();

                        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
                        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

                        System.out.println("=== MemoryMXBean ===");
                        System.out.println(">> Heap");
                        System.out.println("  Init: " + formatBytes(heapMemoryUsage.getInit()));
                        System.out.println("  Used: " + formatBytes(heapMemoryUsage.getUsed()));
                        System.out.println("  Committed: " + formatBytes(heapMemoryUsage.getCommitted()));
                        System.out.println("  Max: " + formatBytes(heapMemoryUsage.getMax()));
                        System.out.println("");
                        System.out.println(">> Non-Heap");
                        System.out.println("  Init: " + formatBytes(nonHeapMemoryUsage.getInit()));
                        System.out.println("  Used: " + formatBytes(nonHeapMemoryUsage.getUsed()));
                        System.out.println("  Committed: " + formatBytes(nonHeapMemoryUsage.getCommitted()));
                        System.out.println("  Max: " + formatBytes(nonHeapMemoryUsage.getMax()));
                        System.out.println("");
                        System.out.println("");

                        // todo MemoryMXBean notifications

                        System.out.println("=== MemoryManagerMXBeans ===");
                        for (MemoryManagerMXBean memoryManagerMXBean : memoryManagerMXBeans) {
                            System.out.println(memoryManagerMXBean.getName() + " --> " + Arrays.toString(memoryManagerMXBean.getMemoryPoolNames()));
                        }
                        System.out.println("");
                        System.out.println("");

                        System.out.println("=== MemoryPoolMXBeans ===");
                        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
                            System.out.println(">> " + memoryPoolMXBean.getName());
                            System.out.println("  Type: " + memoryPoolMXBean.getType().name());
                            System.out.println("  Managers: " + Arrays.toString(memoryPoolMXBean.getMemoryManagerNames()));
                            if (memoryPoolMXBean.isUsageThresholdSupported()) {
                                System.out.println("  Usage Threshold: " + formatBytes(memoryPoolMXBean.getUsageThreshold()));
                                System.out.println("  Usage Threshold Count: " + memoryPoolMXBean.getUsageThresholdCount());
                            }
                            if (memoryPoolMXBean.isCollectionUsageThresholdSupported()) {
                                System.out.println("  Collection Usage Threshold: " + formatBytes(memoryPoolMXBean.getCollectionUsageThreshold()));
                                System.out.println("  Collection Usage Threshold Count: " + memoryPoolMXBean.getCollectionUsageThresholdCount());
                            }
                            System.out.println("  Usage:");
                            System.out.println("    Init: " + formatBytes(memoryPoolMXBean.getUsage().getInit()));
                            System.out.println("    Used: " + formatBytes(memoryPoolMXBean.getUsage().getUsed()));
                            System.out.println("    Committed: " + formatBytes(memoryPoolMXBean.getUsage().getCommitted()));
                            System.out.println("    Max: " + formatBytes(memoryPoolMXBean.getUsage().getMax()));
                            System.out.println("  Peak Usage:");
                            System.out.println("    Init: " + formatBytes(memoryPoolMXBean.getPeakUsage().getInit()));
                            System.out.println("    Used: " + formatBytes(memoryPoolMXBean.getPeakUsage().getUsed()));
                            System.out.println("    Committed: " + formatBytes(memoryPoolMXBean.getPeakUsage().getCommitted()));
                            System.out.println("    Max: " + formatBytes(memoryPoolMXBean.getPeakUsage().getMax()));
                            if (memoryPoolMXBean.getCollectionUsage() != null) {
                                System.out.println("  Collection Usage:");
                                System.out.println("    Init: " + formatBytes(memoryPoolMXBean.getCollectionUsage().getInit()));
                                System.out.println("    Used: " + formatBytes(memoryPoolMXBean.getCollectionUsage().getUsed()));
                                System.out.println("    Committed: " + formatBytes(memoryPoolMXBean.getCollectionUsage().getCommitted()));
                                System.out.println("    Max: " + formatBytes(memoryPoolMXBean.getCollectionUsage().getMax()));
                            }
                            System.out.println("");
                        }
                    });
                })
                .build()
        );
    }

    public static String formatBytes(long bytes) {
        if (bytes == 0) {
            return "0 bytes";
        }
        String[] sizes = new String[]{"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        int sizeIndex = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f", bytes / Math.pow(1024, sizeIndex)) + " " + sizes[sizeIndex];
    }

}
