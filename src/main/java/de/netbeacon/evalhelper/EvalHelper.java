/*
 *     Copyright 2020 Horstexplorer @ https://www.netbeacon.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.netbeacon.evalhelper;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class helps executing code from a source file
 *
 * @author horstexplorer
 */
public class EvalHelper {

    public static final Logger logger = LoggerFactory.getLogger(EvalHelper.class);
    public static final Pattern pattern = Pattern.compile("(public class)(.*?)(\\{)", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    public static final String root = "./eval/";

    /**
     * Starts execution of a given source file
     *
     * - 1 arg expected: path to the source file
     * - will be moved and compiled in a temp dir
     * - must contain one public class containing the default main method
     * - temp dir will be removed after execution
     *
     * @param args path to file
     */
    public static void main(String... args){
        logger.info("--- EvalHelper");
        try{
            // check args
            if(args.length != 1){
                throw new Exception("Invalid Args");
            }
            // check file
            File source = new File(args[0]);
            if(!source.exists()){
                throw new Exception("File Not Found");
            }
            logger.info("Preparing Env...");
            // prepare env if not exists
            File rootFile = new File(root);
            if(!rootFile.exists()){if(!rootFile.mkdirs()){throw new Exception("Could not create temp env");}}
            // create temp dir
            long tmpID = Math.abs(new Random().nextLong());
            File tmpRoot = new File(root+tmpID+"/");
            if(!tmpRoot.exists()){if(!tmpRoot.mkdirs()){throw new Exception("Could not create temp dir");}}
            else{FileUtils.cleanDirectory(tmpRoot);}
            // move file
            File tempFile = new File(root+tmpID+"/tmp");
            if(!source.renameTo(tempFile)){
                throw new Exception("Could not move file to temp dir");
            }
            logger.info("Processing file...");
            String content = new String(Files.readAllBytes(tempFile.toPath()));
            Matcher matcher = pattern.matcher(content);
            boolean found = false;
            String mainFileName = "tmp";
            while(matcher.find()){
                if(found){
                    throw new Exception("File cant contain multiple classes");
                }
                if(matcher.groupCount() != 3){
                    throw new Exception("Invalid Data");
                }
                mainFileName = matcher.group(2).trim();
                found = true;
            }
            if(!found){
                throw new Exception("Could not find public class");
            }
            // move file to final name
            File compileFile = new File(root+tmpID+"/"+mainFileName+".java");
            if(!tempFile.renameTo(compileFile)){
                throw new Exception("Could not rename temp file");
            }
            // compile
            logger.info("Compiling classes...");
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            compiler.run(null, null, null, compileFile.getAbsolutePath());
            // load classes
            logger.info("Loading classes...");
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{tmpRoot.toURI().toURL()}, EvalHelper.class.getClassLoader());
            // start main class
            logger.info("Executing main method of class "+mainFileName+"...");
            System.out.println();
            Class<?> clazz = Class.forName(mainFileName, true, urlClassLoader);
            Method method = clazz.getMethod("main", String[].class);
            method.invoke(null, (Object) new String[]{});
            System.out.println();
            // finish
            logger.info("Execution finished");
            FileUtils.deleteDirectory(tmpRoot);
            System.exit(0);
        }catch (Exception e){
            logger.error("An Error Occurred: ",e);
            System.exit(-1);
        }
    }
}
