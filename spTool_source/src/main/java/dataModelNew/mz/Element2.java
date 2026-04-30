/*
 *  Copyright 2026 Matthias Elinkmann, spTool3
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dataModelNew.mz;

import gui.dialog.FillCollection;
import gui.dialog.Fillable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.Statistics;
import util.ArrUtils;

public enum Element2 implements Fillable<Element2>, FillCollection<Element2> {

  H("Hydrogen", 1, new int[]{1, 2, 3}, new double[]{0.999885, 0.000115, 0.0},
      new double[]{1.007825, 2.014102, 3.016049}, 1, 1, false, false),
  He("Helium", 2, new int[]{3, 4}, new double[]{0.000137, 0.999863},
      new double[]{3.016029, 4.002603}, 1, 18, false, false),
  Li("Lithium", 3, new int[]{6, 7}, new double[]{0.0759, 0.9241}, new double[]{6.015122, 7.016004},
      2, 1, false, false),
  Be("Beryllium", 4, new int[]{9}, new double[]{1.0}, new double[]{9.012182}, 2, 2, false, false),
  B("Boron", 5, new int[]{10, 11}, new double[]{0.199, 0.801}, new double[]{10.012937, 11.009305},
      2, 13, false, false),
  C("Carbon", 6, new int[]{12, 13, 14}, new double[]{0.9893, 0.0107, 0.0},
      new double[]{12.000000, 13.003355, 14.003242}, 2, 14, false, false),
  N("Nitrogen", 7, new int[]{14, 15}, new double[]{0.99632, 0.00368},
      new double[]{14.003074, 15.000109}, 2, 15, false, false),
  O("Oxygen", 8, new int[]{16, 17, 18}, new double[]{0.99757, 0.00038, 0.00205},
      new double[]{15.994915, 16.999132, 17.999160}, 2, 16, false, false),
  F("Fluorine", 9, new int[]{19}, new double[]{1.0}, new double[]{18.998403}, 2, 17, false, false),
  Ne("Neon", 10, new int[]{20, 21, 22}, new double[]{0.9048, 0.0027, 0.0925},
      new double[]{19.992440, 20.993847, 21.991386}, 2, 18, false, false),
  Na("Sodium", 11, new int[]{23}, new double[]{1.0}, new double[]{22.989770}, 3, 1, false, false),
  Mg("Magnesium", 12, new int[]{24, 25, 26}, new double[]{0.7899, 0.1, 0.1101},
      new double[]{23.985042, 24.985837, 25.982593}, 3, 2, false, false),
  Al("Aluminum", 13, new int[]{27}, new double[]{1.0}, new double[]{26.981538}, 3, 13, false,
      false),
  Si("Silicon", 14, new int[]{28, 29, 30}, new double[]{0.922297, 0.046832, 0.030872},
      new double[]{27.976927, 28.976495, 29.973770}, 3, 14, false, false),
  P("Phosphorus", 15, new int[]{31}, new double[]{1.0}, new double[]{30.973762}, 3, 15, false,
      false),
  S("Sulphur", 16, new int[]{32, 33, 34, 36}, new double[]{0.9493, 0.0076, 0.0429, 0.0002},
      new double[]{31.972071, 32.971458, 33.967867, 35.967081}, 3, 16, false, false),
  Cl("Chlorine", 17, new int[]{35, 37}, new double[]{0.7578, 0.2422},
      new double[]{34.968853, 36.965903}, 3, 17, false, false),
  Ar("Argon", 18, new int[]{36, 38, 40}, new double[]{0.003365, 0.000632, 0.996003},
      new double[]{35.967546, 37.962732, 39.962383}, 3, 18, false, false),
  K("Potassium", 19, new int[]{39, 40, 41}, new double[]{0.932581, 0.000117, 0.067302},
      new double[]{38.963707, 39.963999, 40.961826}, 4, 1, false, false),
  Ca("Calcium", 20, new int[]{40, 42, 43, 44, 46, 48},
      new double[]{0.96941, 0.00647, 0.00135, 0.02086, 0.00004, 0.00187},
      new double[]{39.962591, 41.958618, 42.958767, 43.955481, 45.953693, 47.952534}, 4, 2, false,
      false),
  Sc("Scandium", 21, new int[]{45}, new double[]{1.0}, new double[]{44.955910}, 4, 3, false, false),
  Ti("Titanium", 22, new int[]{46, 47, 48, 49, 50},
      new double[]{0.0825, 0.0744, 0.7372, 0.0541, 0.0518},
      new double[]{45.952629, 46.951764, 47.947947, 48.947871, 49.944792}, 4, 4, false, false),
  V("Vanadium", 23, new int[]{50, 51}, new double[]{0.0025, 0.9975},
      new double[]{49.947163, 50.943964}, 4, 5, false, false),
  Cr("Chromium", 24, new int[]{50, 52, 53, 54}, new double[]{0.04345, 0.83789, 0.09501, 0.02365},
      new double[]{49.946050, 51.940512, 52.940654, 53.938885}, 4, 6, false, false),
  Mn("Manganese", 25, new int[]{55}, new double[]{1.0}, new double[]{54.938050}, 4, 7, false,
      false),
  Fe("Iron", 26, new int[]{54, 56, 57, 58}, new double[]{0.05845, 0.91754, 0.02119, 0.00282},
      new double[]{53.939615, 55.934942, 56.935399, 57.933280}, 4, 8, false, false),
  Co("Cobalt", 27, new int[]{59}, new double[]{1.0}, new double[]{58.933200}, 4, 9, false, false),
  Ni("Nickel", 28, new int[]{58, 60, 61, 62, 64},
      new double[]{0.680769, 0.262231, 0.011399, 0.036345, 0.009256},
      new double[]{57.935348, 59.930791, 60.931060, 61.928349, 63.927970}, 4, 10, false, false),
  Cu("Copper", 29, new int[]{63, 65}, new double[]{0.6917, 0.3083},
      new double[]{62.929601, 64.927794}, 4, 11, false, false),
  Zn("Zinc", 30, new int[]{64, 66, 67, 68, 70},
      new double[]{0.4863, 0.2790, 0.0410, 0.1875, 0.0062},
      new double[]{63.929147, 65.926037, 66.927131, 67.924848, 69.925325}, 4, 12, false, false),
  Ga("Gallium", 31, new int[]{69, 71}, new double[]{0.60108, 0.39892},
      new double[]{68.925581, 70.924705}, 4, 13, false, false),
  Ge("Germanium", 32, new int[]{70, 72, 73, 74, 76},
      new double[]{0.2084, 0.2754, 0.0773, 0.3628, 0.0761},
      new double[]{69.924250, 71.922076, 72.923459, 73.921178, 75.921403}, 4, 14, false, false),
  As("Arsenic", 33, new int[]{75}, new double[]{1.0}, new double[]{74.921596}, 4, 15, false, false),
  Se("Selenium", 34, new int[]{74, 76, 77, 78, 80, 82},
      new double[]{0.0089, 0.0937, 0.0763, 0.2377, 0.4961, 0.0873},
      new double[]{73.922477, 75.919214, 76.919915, 77.917310, 79.916522, 81.916700}, 4, 16, false,
      false),
  Br("Bromine", 35, new int[]{79, 81}, new double[]{0.5069, 0.4931},
      new double[]{78.918338, 80.916291}, 4, 17, false, false),
  Kr("Krypton", 36, new int[]{78, 80, 82, 83, 84, 86},
      new double[]{0.0035, 0.0228, 0.1158, 0.1149, 0.5700, 0.1730},
      new double[]{77.920386, 79.916378, 81.913485, 82.914136, 83.911507, 85.910610}, 4, 18, false,
      false),
  Rb("Rubidium", 37, new int[]{85, 87}, new double[]{0.7217, 0.2783},
      new double[]{84.911789, 86.909183}, 5, 1, false, false),
  Sr("Strontium", 38, new int[]{84, 86, 87, 88}, new double[]{0.0056, 0.0986, 0.0700, 0.8258},
      new double[]{83.913425, 85.909262, 86.908879, 87.905614}, 5, 2, false, false),
  Y("Yttrium", 39, new int[]{89}, new double[]{1.0}, new double[]{88.905848}, 5, 3, false, false),
  Zr("Zirconium", 40, new int[]{90, 91, 92, 94, 96},
      new double[]{0.5145, 0.1122, 0.1715, 0.1738, 0.0280},
      new double[]{89.904704, 90.905645, 91.905040, 93.906316, 95.908276}, 5, 4, false, false),
  Nb("Niobium", 41, new int[]{93}, new double[]{1.0}, new double[]{92.906378}, 5, 5, false, false),
  Mo("Molybdenum", 42, new int[]{92, 94, 95, 96, 97, 98, 100},
      new double[]{0.1484, 0.0925, 0.1592, 0.1668, 0.0955, 0.2413, 0.0963},
      new double[]{91.906810, 93.905088, 94.905841, 95.904679, 96.906021, 97.905408, 99.907477}, 5,
      6, false, false),
  Tc("Technetium", 43, new int[]{98}, new double[]{0.0}, new double[]{97.907216}, 5, 7, false,
      false),
  Ru("Ruthenium", 44, new int[]{96, 98, 99, 100, 101, 102, 104},
      new double[]{0.0554, 0.0187, 0.1276, 0.1260, 0.1706, 0.3155, 0.1862},
      new double[]{95.907598, 97.905287, 98.905939, 99.904220, 100.905582, 101.904350, 103.905430},
      5, 8, false, false),
  Rh("Rhodium", 45, new int[]{103}, new double[]{1.0}, new double[]{102.905504}, 5, 9, false,
      false),
  Pd("Palladium", 46, new int[]{102, 104, 105, 106, 108, 110},
      new double[]{0.0102, 0.1114, 0.2233, 0.2733, 0.2646, 0.1172},
      new double[]{101.905608, 103.904035, 104.905084, 105.903483, 107.903894, 109.905152}, 5, 10,
      false, false),
  Ag("Silver", 47, new int[]{107, 109}, new double[]{0.51839, 0.48161},
      new double[]{106.905093, 108.904756}, 5, 11, false, false),
  Cd("Cadmium", 48, new int[]{106, 108, 110, 111, 112, 113, 114, 116},
      new double[]{0.0125, 0.0089, 0.1249, 0.1280, 0.2413, 0.1222, 0.2873, 0.0749},
      new double[]{105.906458, 107.904183, 109.903006, 110.904182, 111.902757, 112.904401,
          113.903358, 115.904755}, 5, 12, false, false),
  In("Indium", 49, new int[]{113, 115}, new double[]{0.0429, 0.9571},
      new double[]{112.904061, 114.903878}, 5, 13, false, false),
  Sn("Tin", 50, new int[]{112, 114, 115, 116, 117, 118, 119, 120, 122, 124},
      new double[]{0.0097, 0.0066, 0.0034, 0.1454, 0.0768, 0.2422, 0.0859, 0.3258, 0.0463, 0.0579},
      new double[]{111.904821, 113.902782, 114.903346, 115.901744, 116.902954, 117.901606,
          118.903309, 119.902197, 121.903440, 123.905275}, 5, 14, false, false),
  Sb("Antimony", 51, new int[]{121, 123}, new double[]{0.5721, 0.4279},
      new double[]{120.903818, 122.904216}, 5, 15, false, false),
  Te("Tellurium", 52, new int[]{120, 122, 123, 124, 125, 126, 128, 130},
      new double[]{0.0009, 0.0255, 0.0089, 0.0474, 0.0707, 0.1884, 0.3174, 0.3408},
      new double[]{119.904020, 121.903047, 122.904273, 123.902819, 124.904425, 125.903306,
          127.904461, 129.906223}, 5, 16, false, false),
  I("Iodine", 53, new int[]{127}, new double[]{1.0}, new double[]{126.904468}, 5, 17, false, false),
  Xe("Xenon", 54, new int[]{124, 126, 128, 129, 130, 131, 132, 134, 136},
      new double[]{0.0009, 0.0009, 0.0192, 0.2644, 0.0408, 0.2118, 0.2689, 0.1044, 0.0887},
      new double[]{123.905896, 125.904269, 127.903530, 128.904779, 129.903508, 130.905082,
          131.904154, 133.905395, 135.907220}, 5, 18, false, false),
  Cs("Cesium", 55, new int[]{133}, new double[]{1.0}, new double[]{132.905447}, 6, 1, false, false),
  Ba("Barium", 56, new int[]{130, 132, 134, 135, 136, 137, 138},
      new double[]{0.00106, 0.00101, 0.02417, 0.06592, 0.07854, 0.11232, 0.71698},
      new double[]{129.906310, 131.905056, 133.904503, 134.905683, 135.904570, 136.905821,
          137.905241}, 6, 2, false, false),
  La("Lanthanum", 57, new int[]{138, 139}, new double[]{0.0009, 0.9991},
      new double[]{137.907107, 138.906348}, 6, 3, false, false),
  Ce("Cerium", 58, new int[]{136, 138, 140, 142}, new double[]{0.00185, 0.00251, 0.8845, 0.11114},
      new double[]{135.907144, 137.905986, 139.905434, 141.909240}, 6, 4, true, false),
  Pr("Praseodymium", 59, new int[]{141}, new double[]{1.0}, new double[]{140.907648}, 6, 5, true,
      false),
  Nd("Neodymium", 60, new int[]{142, 143, 144, 145, 146, 148, 150},
      new double[]{0.272, 0.122, 0.238, 0.083, 0.172, 0.057, 0.056},
      new double[]{141.907719, 142.909810, 143.910083, 144.912569, 145.913112, 147.916889,
          149.920887}, 6, 6, true, false),
  Pm("Promethium ", 61, new int[]{147},
      new double[]{100}, new double[]{146.9151449}, 6,7,true, false),
  Sm("Samarium", 62, new int[]{144, 147, 148, 149, 150, 152, 154},
      new double[]{0.0307, 0.1499, 0.1124, 0.1382, 0.0738, 0.2675, 0.2275},
      new double[]{143.911995, 146.914893, 147.914818, 148.917180, 149.917271, 151.919728,
          153.922205}, 6, 8, true, false),
  Eu("Europium", 63, new int[]{151, 153}, new double[]{0.4781, 0.5219},
      new double[]{150.919846, 152.921226}, 6, 9, true, false),
  Gd("Gadolinium", 64, new int[]{152, 154, 155, 156, 157, 158, 160},
      new double[]{0.002, 0.0218, 0.1480, 0.2047, 0.1565, 0.2484, 0.2186},
      new double[]{151.919788, 153.920862, 154.922619, 155.922120, 156.923957, 157.924101,
          159.927051}, 6, 10, true, false),
  Tb("Terbium", 65, new int[]{159}, new double[]{1.0}, new double[]{158.925343}, 6, 11, true,
      false),
  Dy("Dysprosium", 66, new int[]{156, 158, 160, 161, 162, 163, 164},
      new double[]{0.0006, 0.0010, 0.0234, 0.1891, 0.2551, 0.2490, 0.2818},
      new double[]{155.924278, 157.924405, 159.925194, 160.926930, 161.926795, 162.928728,
          163.929171}, 6, 12, true, false),
  Ho("Holmium", 67, new int[]{165}, new double[]{1.0}, new double[]{164.930319}, 6, 13, true,
      false),
  Er("Erbium", 68, new int[]{162, 164, 166, 167, 168, 170},
      new double[]{0.0014, 0.0161, 0.3361, 0.2293, 0.2678, 0.1493},
      new double[]{161.928775, 163.929197, 165.930290, 166.932045, 167.932368, 169.935460}, 6, 14,
      true, false),
  Tm("Thulium", 69, new int[]{169}, new double[]{1.0}, new double[]{168.934211}, 6, 15, true,
      false),
  Yb("Ytterbium", 70, new int[]{168, 170, 171, 172, 173, 174, 176},
      new double[]{0.0013, 0.0304, 0.1428, 0.2183, 0.1613, 0.3183, 0.1276},
      new double[]{167.933894, 169.934759, 170.936322, 171.936378, 172.938207, 173.938858,
          175.942568}, 6, 16, true, false),
  Lu("Lutetium", 71, new int[]{175, 176}, new double[]{0.9741, 0.0259},
      new double[]{174.940768, 175.942682}, 6, 17, true, false),
  Hf("Hafnium", 72, new int[]{174, 176, 177, 178, 179, 180},
      new double[]{0.0016, 0.0526, 0.1860, 0.2728, 0.1362, 0.3508},
      new double[]{173.940040, 175.941402, 176.943220, 177.943698, 178.945815, 179.946549}, 7, 4,
      false, false),
  Ta("Tantalum", 73, new int[]{180, 181}, new double[]{0.00012, 0.99988},
      new double[]{179.947466, 180.947996}, 7, 5, false, false),
  W("Tungsten", 74, new int[]{180, 182, 183, 184, 186},
      new double[]{0.0012, 0.2650, 0.1431, 0.3064, 0.2843},
      new double[]{179.946706, 181.948206, 182.950224, 183.950933, 185.954362}, 7, 6, false, false),
  Re("Rhenium", 75, new int[]{185, 187}, new double[]{0.3740, 0.6260},
      new double[]{184.952956, 186.955751}, 7, 7, false, false),
  Os("Osmium", 76, new int[]{184, 186, 187, 188, 189, 190, 192},
      new double[]{0.0002, 0.0159, 0.0196, 0.1324, 0.1615, 0.2626, 0.4078},
      new double[]{183.952491, 185.953838, 186.955748, 187.955836, 188.958145, 189.958445,
          191.961479}, 7, 8, false, false),
  Ir("Iridium", 77, new int[]{191, 193}, new double[]{0.373, 0.627},
      new double[]{190.960591, 192.962924}, 7, 9, false, false),
  Pt("Platinum", 78, new int[]{190, 192, 194, 195, 196, 198},
      new double[]{0.00014, 0.00782, 0.32967, 0.33832, 0.25242, 0.07163},
      new double[]{189.959930, 191.961035, 193.962664, 194.964774, 195.964935, 197.967876}, 7, 10,
      false, false),
  Au("Gold", 79, new int[]{197}, new double[]{1.0}, new double[]{196.966552}, 7, 11, false, false),
  Hg("Mercury", 80, new int[]{196, 198, 199, 200, 201, 202, 204},
      new double[]{0.0015, 0.0997, 0.1687, 0.2310, 0.1318, 0.2986, 0.0687},
      new double[]{195.965815, 197.966752, 198.968262, 199.968309, 200.970285, 201.970626,
          203.973476}, 7, 12, false, false),
  Tl("Thallium", 81, new int[]{203, 205}, new double[]{0.29524, 0.70476},
      new double[]{202.972329, 204.974412}, 7, 13, false, false),
  Pb("Lead", 82, new int[]{204, 206, 207, 208}, new double[]{0.014, 0.241, 0.221, 0.524},
      new double[]{203.973029, 205.974449, 206.975881, 207.976636}, 7, 14, false, false),
  Bi("Bismuth", 83, new int[]{209}, new double[]{1.0}, new double[]{208.980383}, 7, 15, false,
      false),
  Po("Polonium", 84, new int[]{209}, new double[]{0.0}, new double[]{208.982416}, 7, 16, false,
      false),
  At("Astatine", 85, new int[]{210}, new double[]{0.0}, new double[]{209.987131}, 7, 17, false,
      false),
  Rn("Radon", 86, new int[]{222}, new double[]{0.0}, new double[]{222.017570}, 7, 18, false, false),
  Fr("Francium", 87, new int[]{223}, new double[]{0.0}, new double[]{223.019731}, 8, 1, false,
      false),
  Ra("Radium", 88, new int[]{226}, new double[]{0.0}, new double[]{226.025403}, 8, 2, false, false),
  Ac("Actinium", 89, new int[]{227}, new double[]{0.0}, new double[]{227.027747}, 8, 3, false,
      false),
  Th("Thorium", 90, new int[]{232}, new double[]{1.0}, new double[]{232.038050}, 8, 4, false, true),
  Pa("Protactinium", 91, new int[]{231}, new double[]{1.0}, new double[]{231.035879}, 8, 5, false,
      true),
  U("Uranium", 92, new int[]{234, 235, 238}, new double[]{0.00055, 0.0072, 0.992745},
      new double[]{234.040946, 235.043923, 238.050783}, 8, 6, false, true),
  Np("Neptunium", 93, new int[]{237}, new double[]{0.0}, new double[]{237.048167}, 8, 7, false,
      true),
  Pu("Plutonium", 94, new int[]{244}, new double[]{0.0}, new double[]{244.064198}, 8, 8, false,
      true),
  Am("Americium", 95, new int[]{243}, new double[]{0.0}, new double[]{243.061373}, 8, 9, false,
      true),
  Cm("Curium", 96, new int[]{247}, new double[]{0.0}, new double[]{247.070347}, 8, 10, false, true),
  Bk("Berkelium", 97, new int[]{247}, new double[]{0.0}, new double[]{247.070299}, 8, 11, false,
      true),
  Cf("Californium", 98, new int[]{251}, new double[]{0.0}, new double[]{251.079580}, 8, 12, false,
      true),
  Es("Einsteinium", 99, new int[]{252}, new double[]{0.0}, new double[]{252.082972}, 8, 13, false,
      true),
  Fm("Fermium", 100, new int[]{257}, new double[]{0.0}, new double[]{257.095110}, 8, 14, false,
      true),
  Md("Mendelevium", 101, new int[]{258}, new double[]{0.0}, new double[]{258.098431}, 8, 15, false,
      true),
  No("Nobelium", 102, new int[]{259}, new double[]{0.0}, new double[]{259.100000}, 8, 16, false,
      true),
  Lr("Lawrencium", 103, new int[]{262}, new double[]{0.0}, new double[]{262.110000}, 8, 17, false,
      true),
  Rf("Rutherfordium", 104, new int[]{267}, new double[]{0.0}, new double[]{267.122000}, 9, 4, false,
      false),
  Db("Dubnium", 105, new int[]{270}, new double[]{0.0}, new double[]{270.123000}, 9, 5, false,
      false),
  Sg("Seaborgium", 106, new int[]{271}, new double[]{0.0}, new double[]{271.133000}, 9, 6, false,
      false),
  Bh("Bohrium", 107, new int[]{270}, new double[]{0.0}, new double[]{270.133000}, 9, 7, false,
      false),
  Hs("Hassium", 108, new int[]{277}, new double[]{0.0}, new double[]{277.153000}, 9, 8, false,
      false),
  Mt("Meitnerium", 109, new int[]{276}, new double[]{0.0}, new double[]{276.153000}, 9, 9, false,
      false),
  Ds("Darmstadtium", 110, new int[]{281}, new double[]{0.0}, new double[]{281.168000}, 9, 10, false,
      false),
  Rg("Roentgenium", 111, new int[]{280}, new double[]{0.0}, new double[]{280.160000}, 9, 11, false,
      false),
  Cn("Copernicium", 112, new int[]{285}, new double[]{0.0}, new double[]{285.175000}, 9, 12, false,
      false),
  Nh("Nihonium", 113, new int[]{284}, new double[]{0.0}, new double[]{284.182000}, 9, 13, false,
      false),
  Fl("Flerovium", 114, new int[]{289}, new double[]{0.0}, new double[]{289.189000}, 9, 14, false,
      false),
  Mc("Moscovium", 115, new int[]{288}, new double[]{0.0}, new double[]{288.188000}, 9, 15, false,
      false),
  Lv("Livermorium", 116, new int[]{293}, new double[]{0.0}, new double[]{293.201000}, 9, 16, false,
      false),
  Ts("Tennessine", 117, new int[]{294}, new double[]{0.0}, new double[]{294.210000}, 9, 17, false,
      false),
  Og("Oganesson", 118, new int[]{294}, new double[]{0.0}, new double[]{294.214000}, 9, 18, false,
      false);


  public static final Logger LOGGER = LogManager.getLogger(Element2.class.getName());

  private final String name;
  private final int atomicNumber;
  private final int[] isotopes;
  private final double[] exactMasses;
  private final double[] abundances;
  private final int row;
  private final int column;
  private final boolean isLanthanide;
  private final boolean isActinide;

  Element2(String name, int atomicNumber, int[] isotopes, double[] abundances, double[] exactMasses,
      int row, int column,
      boolean isLanthanide, boolean isActinide) {
    this.name = name;
    this.atomicNumber = atomicNumber;
    this.isotopes = isotopes;
    this.exactMasses = exactMasses;
    this.abundances = abundances;
    this.row = row;
    this.column = column;
    this.isLanthanide = isLanthanide;
    this.isActinide = isActinide;
  }

  public String getName() {
    return name;
  }

  public String getSymbol() {
    return toString();
  }

  public int getAtomicNumber() {
    return atomicNumber;
  }

  // TODO: Add a factory for the buttons since there is a lot of doubling here.
  public BorderPane getPToE() {
    HashMap<Element2, Boolean> map = new LinkedHashMap<>();

    final BorderPane borderPane = new BorderPane();
    final HBox hBox = new HBox(10);
    final VBox vBox = new VBox(10);
    vBox.setPadding(new Insets(10));

    final HBox lanthanides = new HBox(10);
    final HBox actinides = new HBox(10);

    final List<VBox> columns = new ArrayList<>();
    for (int i = 1; i <= 18; i++) {
      columns.add(new VBox(10));
      columns.forEach(b -> b.setAlignment(Pos.BOTTOM_CENTER));
    }

    hBox.getChildren().addAll(columns);
    vBox.getChildren().addAll(new Separator(Orientation.HORIZONTAL), lanthanides, actinides);

    for (int i = 0; i < values().length; i++) {
      Element2 element = values()[i];
      if (element.isActinide) {
        ToggleButton btn = new ToggleButton(element.toString());
        btn.setPrefWidth(37);
        btn.setOnMouseMoved(e -> {
          if (e.isControlDown()) {
            btn.setSelected(true);
          } else if (e.isAltDown()) {
            btn.setSelected(false);
          }
        });
        btn.selectedProperty().addListener(e -> {
          if (btn.isSelected()) {
            map.put(element, true);
            String boldBlueStyle = "-fx-font-weight: bold; -fx-background-color: blue; -fx-text-fill: white;";
            btn.setStyle(boldBlueStyle);
          } else {
            map.put(element, false);
            String defaultStyle = ""; // Reset to default styles
            btn.setStyle(defaultStyle);
          }
        });
        actinides.getChildren().add(btn);
      } else if (element.isLanthanide) {
        ToggleButton btn = new ToggleButton(element.toString());
        btn.setPrefWidth(37);
        btn.setOnMouseMoved(e -> {
          if (e.isControlDown()) {
            btn.setSelected(true);
          } else if (e.isAltDown()) {
            btn.setSelected(false);
          }
        });
        btn.selectedProperty().addListener(e -> {
          if (btn.isSelected()) {
            map.put(element, true);
            String boldBlueStyle = "-fx-font-weight: bold; -fx-background-color: blue; -fx-text-fill: white;";
            btn.setStyle(boldBlueStyle);
          } else {
            map.put(element, false);
            String defaultStyle = ""; // Reset to default styles
            btn.setStyle(defaultStyle);
          }
        });
        lanthanides.getChildren().add(btn);
      } else {
        ToggleButton btn = new ToggleButton(element.toString());
        btn.setPrefWidth(37);
        btn.setOnMouseMoved(e -> {
          if (e.isControlDown()) {
            btn.setSelected(true);
          } else if (e.isAltDown()) {
            btn.setSelected(false);
          }
        });
        btn.selectedProperty().addListener(e -> {
          if (btn.isSelected()) {
            map.put(element, true);
            String boldBlueStyle = "-fx-font-weight: bold; -fx-background-color: blue; -fx-text-fill: white;";
            btn.setStyle(boldBlueStyle);
          } else {
            map.put(element, false);
            String defaultStyle = ""; // Reset to default styles
            btn.setStyle(defaultStyle);
          }
        });
        int col = element.column;
        columns.get(col - 1).getChildren().add(btn);
      }
    }
    borderPane.setCenter(hBox);
    borderPane.setPadding(new Insets(10));
    borderPane.setBottom(vBox);

    return borderPane;
  }

  public List<Isotope> getIsotopes() {
    final List<Isotope> isotopeList = new ArrayList<>();
    for (int i = 0; i < isotopes.length; i++) {
      // TODO replace null with "this"
      isotopeList.add(new Isotope(null, isotopes[i], exactMasses[i], abundances[i]));
    }
    return isotopeList;
  }


  public HashMap<Isotope, Double> getRandomizedIsotopeSignalLvl(double elementSignal,
      double isotopicUncertainty) {
    HashMap<Isotope, Double> isotopicFractions = new LinkedHashMap<>();

    List<Isotope> isotopes = getIsotopes();
    double[] fractions = new double[isotopes.size()];

    // Calculate, then normalize
    for (int i = 0; i < isotopes.size(); i++) {
      Isotope isotope = isotopes.get(i);
      fractions[i] = Statistics.randomifyPercent(isotope.getAbundance(), isotopicUncertainty);
    }
    ArrUtils.normalizeBySumOverriding(fractions);
    ArrUtils.multiplyOverriding(fractions, elementSignal);

    for (int i = 0; i < isotopes.size(); i++) {
      isotopicFractions.put(isotopes.get(i), fractions[i]);
    }

    return isotopicFractions;
  }

  @Override
  public String getStringValue() {
    return this.name() + " (" + this.getName() + ")";
  }

  @Override
  public boolean isEqual(Fillable<?> thatFillable) {
    return this.getStringValue().equals(thatFillable.getStringValue());
  }

  @Override
  public Element2 unwrap() {
    return this;
  }


  @Override
  public List<Fillable<Element2>> getItems() {
    return Arrays.stream(Element2.values()).collect(Collectors.toList());
  }

  @Override
  public Element2 getMatch(String string, boolean muteError) {
    Element2 match = null;
    boolean parserFailed = true;
    for (Fillable<Element2> item : getItems()) {
      if (string.equals(item.getStringValue())) {
        match = (Element2) item; // Element implements Fillable<Element> :-
        parserFailed = false;
        break;
      }
    }
    if (!muteError && parserFailed) {
      LOGGER.debug("Unable to parse AutoFillable instance for class Element. "
          + "Input string was: '" + string + "'.");
    }
    return match;
  }

  public static List<Isotope> getIsotopes(List<Element2> elements) {
    List<Isotope> isotopes = new ArrayList<>();
    for (Element2 element : elements) {
      isotopes.addAll(element.getIsotopes());
    }
    return isotopes;
  }


}