/*
 * Copyright (c) 2015, ChemID. (http://www.chemid.org)
 *
 * ChemID licenses this file to you under the Apache License V 2.0.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.chemid.structure.dbclient.hmdb;

import org.chemid.structure.common.Constants;
import org.chemid.structure.exception.ChemIDException;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.iterator.IteratingSDFReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class HMDBClient {


    private SDFWriter writer;
    private IAtomContainer container;
    boolean isInitial;
    String savedPath;

    public String readSDF(double lowerMassValue, double upperMassValue, String location) throws ChemIDException {
        savedPath = null;
        isInitial = true;
        Stream<IAtomContainer> stream = null;
        String doc = null;
        doc = getResourceDocument();
        File sdfFile = new File(doc);
        try {
            IteratingSDFReader finalReader = new IteratingSDFReader(
                    new FileInputStream(sdfFile), DefaultChemObjectBuilder.getInstance());
            Iterable<IAtomContainer> iterables = () -> finalReader;
            StreamSupport.stream(iterables.spliterator(), true)
                    .filter(mol -> {
                        double mimw = Double.parseDouble(mol.getProperty(Constants.HMDBConstants.HMDB_MOLECULAR_WEIGHT));
                        return (mimw > lowerMassValue && mimw < upperMassValue);
                    })
                    .forEach((m) -> {
                        try {
                            if (isInitial) {
                                isInitial = false;
                                savedPath = createFile(location);
                            }
                            IAtomContainer container = m;
                            writer.write(container);
                        } catch (Exception e) {
                            new ChemIDException("Error occurred while writing results: " + e.getMessage());
                        }
                    });
            if (writer != null) {
                writer.close();
            }
        } catch (Exception e) {
            new ChemIDException("Error occurred while downloading hmdb: " + e.getMessage());
        }
        return savedPath;
    }

    private String createFile(String location) {
        String savedPath;
        String outputName = new SimpleDateFormat("yyyyMMddhhmm'.sdf'").format(new Date());
        if (location.endsWith("/")) {
            savedPath = location + outputName;
        } else {
            savedPath = location + '/' + outputName;
        }
        File output = new File(savedPath);
        try {
            writer = new SDFWriter(new FileWriter(output));
        } catch (Exception e) {
            new ChemIDException("Error occurred while creating hmdb file: " + e.getMessage());
        }
        return savedPath;
    }


    public String getResourceDocument() throws ChemIDException {
        String docPath = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(Constants.HMDBConstants.HMDB_RESOURCES + Constants.HMDBConstants.HMDB_OUTPUT_FILE);
            docPath = resource.getPath();
        } catch (Exception e) {
            new ChemIDException("Error occurred while loading Resource File: " + e.getMessage());
        }
        return docPath;
    }


}
