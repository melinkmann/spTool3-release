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
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.Statistics;
import util.ArrUtils;
import util.Util;

public enum Element implements Fillable<Element>, FillCollection<Element> {

  H("Hydrogen", 1, new int[]{1, 2, 3}, new double[]{0.999885, 0.000115, 0.0},
      new double[]{1.007825, 2.014102, 3.016049}),
  He("Helium", 2, new int[]{3, 4}, new double[]{0.000137, 0.999863},
      new double[]{3.016029, 4.002603}),
  Li("Lithium", 3, new int[]{6, 7}, new double[]{0.0759, 0.9241}, new double[]{6.015122, 7.016004}),
  Be("Beryllium", 4, new int[]{9}, new double[]{1.0}, new double[]{9.012182}),
  B("Boron", 5, new int[]{10, 11}, new double[]{0.199, 0.801}, new double[]{10.012937, 11.009305}),
  C("Carbon", 6, new int[]{12, 13}, new double[]{0.9893, 0.0107},
      new double[]{12.000000, 13.003355}),
  N("Nitrogen", 7, new int[]{14, 15}, new double[]{0.99632, 0.00368},
      new double[]{14.003074, 15.000109}),
  O("Oxygen", 8, new int[]{16, 17, 18}, new double[]{0.99757, 0.00038, 0.00205},
      new double[]{15.994915, 16.999132, 17.999160}),
  F("Fluorine", 9, new int[]{19}, new double[]{1.0}, new double[]{18.998403}),
  Ne("Neon", 10, new int[]{20, 21, 22}, new double[]{0.9048, 0.0027, 0.0925},
      new double[]{19.992440, 20.993847, 21.991386}),
  Na("Sodium", 11, new int[]{23}, new double[]{1.0}, new double[]{22.989770}),
  Mg("Magnesium", 12, new int[]{24, 25, 26}, new double[]{0.7899, 0.1, 0.1101},
      new double[]{23.985042, 24.985837, 25.982593}),
  Al("Aluminum", 13, new int[]{27}, new double[]{1.0}, new double[]{26.981538}),
  Si("Silicon", 14, new int[]{28, 29, 30}, new double[]{0.922297, 0.046832, 0.030872},
      new double[]{27.976927, 28.976495, 29.973770}),
  P("Phosphorus", 15, new int[]{31}, new double[]{1.0}, new double[]{30.973762}),
  S("Sulphur", 16, new int[]{32, 33, 34, 36}, new double[]{0.9493, 0.0076, 0.0429, 0.0002},
      new double[]{31.972071, 32.971458, 33.967867, 35.967081}),
  Cl("Chlorine", 17, new int[]{35, 37}, new double[]{0.7578, 0.2422},
      new double[]{34.968853, 36.965903}),
  Ar("Argon", 18, new int[]{36, 38, 40}, new double[]{0.003365, 0.000632, 0.996003},
      new double[]{35.967546, 37.962732, 39.962383}),
  K("Potassium", 19, new int[]{39, 40, 41}, new double[]{0.932581, 0.000117, 0.067302},
      new double[]{38.963707, 39.963999, 40.961826}),
  Ca("Calcium", 20, new int[]{40, 42, 43, 44, 46, 48},
      new double[]{0.96941, 0.00647, 0.00135, 0.02086, 0.00004, 0.00187},
      new double[]{39.962591, 41.958618, 42.958767, 43.955481, 45.953693, 47.952534}),
  Sc("Scandium", 21, new int[]{45}, new double[]{1.0}, new double[]{44.955910}),
  Ti("Titanium", 22, new int[]{46, 47, 48, 49, 50},
      new double[]{0.0825, 0.0744, 0.7372, 0.0541, 0.0518},
      new double[]{45.952629, 46.951764, 47.947947, 48.947871, 49.944792}),
  V("Vanadium", 23, new int[]{50, 51}, new double[]{0.0025, 0.9975},
      new double[]{49.947163, 50.943964}),
  Cr("Chromium", 24, new int[]{50, 52, 53, 54}, new double[]{0.04345, 0.83789, 0.09501, 0.02365},
      new double[]{49.946050, 51.940512, 52.940654, 53.938885}),
  Mn("Manganese", 25, new int[]{55}, new double[]{1.0}, new double[]{54.938050}),
  Fe("Iron", 26, new int[]{54, 56, 57, 58}, new double[]{0.05845, 0.91754, 0.02119, 0.00282},
      new double[]{53.939615, 55.934942, 56.935399, 57.933280}),
  Co("Cobalt", 27, new int[]{59}, new double[]{1.0}, new double[]{58.933200}),
  Ni("Nickel", 28, new int[]{58, 60, 61, 62, 64},
      new double[]{0.680769, 0.262231, 0.011399, 0.036345, 0.009256},
      new double[]{57.935348, 59.930791, 60.931060, 61.928349, 63.927970}),
  Cu("Copper", 29, new int[]{63, 65}, new double[]{0.6917, 0.3083},
      new double[]{62.929601, 64.927794}),
  Zn("Zinc", 30, new int[]{64, 66, 67, 68, 70},
      new double[]{0.4863, 0.2790, 0.0410, 0.1875, 0.0062},
      new double[]{63.929147, 65.926037, 66.927131, 67.924848, 69.925325}),
  Ga("Gallium", 31, new int[]{69, 71}, new double[]{0.60108, 0.39892},
      new double[]{68.925581, 70.924705}),
  Ge("Germanium", 32, new int[]{70, 72, 73, 74, 76},
      new double[]{0.2084, 0.2754, 0.0773, 0.3628, 0.0761},
      new double[]{69.924250, 71.922076, 72.923459, 73.921178, 75.921403}),
  As("Arsenic", 33, new int[]{75}, new double[]{1.0}, new double[]{74.921596}),
  Se("Selenium", 34, new int[]{74, 76, 77, 78, 80, 82},
      new double[]{0.0089, 0.0937, 0.0763, 0.2377, 0.4961, 0.0873},
      new double[]{73.922477, 75.919214, 76.919915, 77.917310, 79.916522, 81.916700}),
  Br("Bromine", 35, new int[]{79, 81}, new double[]{0.5069, 0.4931},
      new double[]{78.918338, 80.916291}),
  Kr("Krypton", 36, new int[]{78, 80, 82, 83, 84, 86},
      new double[]{0.0035, 0.0228, 0.1158, 0.1149, 0.5700, 0.1730},
      new double[]{77.920386, 79.916378, 81.913485, 82.914136, 83.911507, 85.910610}),
  Rb("Rubidium", 37, new int[]{85, 87}, new double[]{0.7217, 0.2783},
      new double[]{84.911789, 86.909183}),
  Sr("Strontium", 38, new int[]{84, 86, 87, 88}, new double[]{0.0056, 0.0986, 0.0700, 0.8258},
      new double[]{83.913425, 85.909262, 86.908879, 87.905614}),
  Y("Yttrium", 39, new int[]{89}, new double[]{1.0}, new double[]{88.905848}),
  Zr("Zirconium", 40, new int[]{90, 91, 92, 94, 96},
      new double[]{0.5145, 0.1122, 0.1715, 0.1738, 0.0280},
      new double[]{89.904704, 90.905645, 91.905040, 93.906316, 95.908276}),
  Nb("Niobium", 41, new int[]{93}, new double[]{1.0}, new double[]{92.906378}),
  Mo("Molybdenum", 42, new int[]{92, 94, 95, 96, 97, 98, 100},
      new double[]{0.1484, 0.0925, 0.1592, 0.1668, 0.0955, 0.2413, 0.0963},
      new double[]{91.906810, 93.905088, 94.905841, 95.904679, 96.906021, 97.905408, 99.907477}),
  Tc("Technetium", 43, new int[]{98}, new double[]{0.0}, new double[]{97.907216}),
  Ru("Ruthenium", 44, new int[]{96, 98, 99, 100, 101, 102, 104},
      new double[]{0.0554, 0.0187, 0.1276, 0.1260, 0.1706, 0.3155, 0.1862},
      new double[]{95.907598, 97.905287, 98.905939, 99.904220, 100.905582, 101.904350, 103.905430}),
  Rh("Rhodium", 45, new int[]{103}, new double[]{1.0}, new double[]{102.905504}),
  Pd("Palladium", 46, new int[]{102, 104, 105, 106, 108, 110},
      new double[]{0.0102, 0.1114, 0.2233, 0.2733, 0.2646, 0.1172},
      new double[]{101.905608, 103.904035, 104.905084, 105.903483, 107.903894, 109.905152}),
  Ag("Silver", 47, new int[]{107, 109}, new double[]{0.51839, 0.48161},
      new double[]{106.905093, 108.904756}),
  Cd("Cadmium", 48, new int[]{106, 108, 110, 111, 112, 113, 114, 116},
      new double[]{0.0125, 0.0089, 0.1249, 0.1280, 0.2413, 0.1222, 0.2873, 0.0749},
      new double[]{105.906458, 107.904183, 109.903006, 110.904182, 111.902757, 112.904401,
          113.903358, 115.904755}),
  In("Indium", 49, new int[]{113, 115}, new double[]{0.0429, 0.9571},
      new double[]{112.904061, 114.903878}),
  Sn("Tin", 50, new int[]{112, 114, 115, 116, 117, 118, 119, 120, 122, 124},
      new double[]{0.0097, 0.0066, 0.0034, 0.1454, 0.0768, 0.2422, 0.0859, 0.3258, 0.0463, 0.0579},
      new double[]{111.904821, 113.902782, 114.903346, 115.901744, 116.902954, 117.901606,
          118.903309, 119.902197, 121.903440, 123.905275}),
  Sb("Antimony", 51, new int[]{121, 123}, new double[]{0.5721, 0.4279},
      new double[]{120.903818, 122.904216}),
  Te("Tellurium", 52, new int[]{120, 122, 123, 124, 125, 126, 128, 130},
      new double[]{0.0009, 0.0255, 0.0089, 0.0474, 0.0707, 0.1884, 0.3174, 0.3408},
      new double[]{119.904020, 121.903047, 122.904273, 123.902819, 124.904425, 125.903306,
          127.904461, 129.906223}),
  I("Iodine", 53, new int[]{127}, new double[]{1.0}, new double[]{126.904468}),
  Xe("Xenon", 54, new int[]{124, 126, 128, 129, 130, 131, 132, 134, 136},
      new double[]{0.0009, 0.0009, 0.0192, 0.2644, 0.0408, 0.2118, 0.2689, 0.1044, 0.0887},
      new double[]{123.905896, 125.904269, 127.903530, 128.904779, 129.903508, 130.905082,
          131.904154, 133.905395, 135.907220}),
  Cs("Cesium", 55, new int[]{133}, new double[]{1.0}, new double[]{132.905447}),
  Ba("Barium", 56, new int[]{130, 132, 134, 135, 136, 137, 138},
      new double[]{0.00106, 0.00101, 0.02417, 0.06592, 0.07854, 0.11232, 0.71698},
      new double[]{129.906310, 131.905056, 133.904503, 134.905683, 135.904570, 136.905821,
          137.905241}),
  La("Lanthanum", 57, new int[]{138, 139}, new double[]{0.0009, 0.9991},
      new double[]{137.907107, 138.906348}),
  Ce("Cerium", 58, new int[]{136, 138, 140, 142}, new double[]{0.00185, 0.00251, 0.8845, 0.11114},
      new double[]{135.907144, 137.905986, 139.905434, 141.909240}),
  Pr("Praseodymium", 59, new int[]{141}, new double[]{1.0}, new double[]{140.907648}),
  Nd("Neodymium", 60, new int[]{142, 143, 144, 145, 146, 148, 150},
      new double[]{0.272, 0.122, 0.238, 0.083, 0.172, 0.057, 0.056},
      new double[]{141.907719, 142.909810, 143.910083, 144.912569, 145.913112, 147.916889,
          149.920887}),
  Pm("Promethium ", 61, new int[]{147},
      new double[]{1}, new double[]{146.9151449}),
  Sm("Samarium", 62, new int[]{144, 147, 148, 149, 150, 152, 154},
      new double[]{0.0307, 0.1499, 0.1124, 0.1382, 0.0738, 0.2675, 0.2275},
      new double[]{143.911995, 146.914893, 147.914818, 148.917180, 149.917271, 151.919728,
          153.922205}),
  Eu("Europium", 63, new int[]{151, 153}, new double[]{0.4781, 0.5219},
      new double[]{150.919846, 152.921226}),
  Gd("Gadolinium", 64, new int[]{152, 154, 155, 156, 157, 158, 160},
      new double[]{0.002, 0.0218, 0.1480, 0.2047, 0.1565, 0.2484, 0.2186},
      new double[]{151.919788, 153.920862, 154.922619, 155.922120, 156.923957, 157.924101,
          159.927051}),
  Tb("Terbium", 65, new int[]{159}, new double[]{1.0}, new double[]{158.925343}),
  Dy("Dysprosium", 66, new int[]{156, 158, 160, 161, 162, 163, 164},
      new double[]{0.0006, 0.0010, 0.0234, 0.1891, 0.2551, 0.2490, 0.2818},
      new double[]{155.924278, 157.924405, 159.925194, 160.926930, 161.926795, 162.928728,
          163.929171}),
  Ho("Holmium", 67, new int[]{165}, new double[]{1.0}, new double[]{164.930319}),
  Er("Erbium", 68, new int[]{162, 164, 166, 167, 168, 170},
      new double[]{0.0014, 0.0161, 0.3361, 0.2293, 0.2678, 0.1493},
      new double[]{161.928775, 163.929197, 165.930290, 166.932045, 167.932368, 169.935460}),
  Tm("Thulium", 69, new int[]{169}, new double[]{1.0}, new double[]{168.934211}),
  Yb("Ytterbium", 70, new int[]{168, 170, 171, 172, 173, 174, 176},
      new double[]{0.0013, 0.0304, 0.1428, 0.2183, 0.1613, 0.3183, 0.1276},
      new double[]{167.933894, 169.934759, 170.936322, 171.936378, 172.938207, 173.938858,
          175.942568}),
  Lu("Lutetium", 71, new int[]{175, 176}, new double[]{0.9741, 0.0259},
      new double[]{174.940768, 175.942682}),
  Hf("Hafnium", 72, new int[]{174, 176, 177, 178, 179, 180},
      new double[]{0.0016, 0.0526, 0.1860, 0.2728, 0.1362, 0.3508},
      new double[]{173.940040, 175.941402, 176.943220, 177.943698, 178.945815, 179.946549}),
  Ta("Tantalum", 73, new int[]{180, 181}, new double[]{0.00012, 0.99988},
      new double[]{179.947466, 180.947996}),
  W("Tungsten", 74, new int[]{180, 182, 183, 184, 186},
      new double[]{0.0012, 0.2650, 0.1431, 0.3064, 0.2843},
      new double[]{179.946706, 181.948206, 182.950224, 183.950933, 185.954362}),
  Re("Rhenium", 75, new int[]{185, 187}, new double[]{0.3740, 0.6260},
      new double[]{184.952956, 186.955751}),
  Os("Osmium", 76, new int[]{184, 186, 187, 188, 189, 190, 192},
      new double[]{0.0002, 0.0159, 0.0196, 0.1324, 0.1615, 0.2626, 0.4078},
      new double[]{183.952491, 185.953838, 186.955748, 187.955836, 188.958145, 189.958445,
          191.961479}),
  Ir("Iridium", 77, new int[]{191, 193}, new double[]{0.373, 0.627},
      new double[]{190.960591, 192.962924}),
  Pt("Platinum", 78, new int[]{190, 192, 194, 195, 196, 198},
      new double[]{0.00014, 0.00782, 0.32967, 0.33832, 0.25242, 0.07163},
      new double[]{189.959930, 191.961035, 193.962664, 194.964774, 195.964935, 197.967876}),
  Au("Gold", 79, new int[]{197}, new double[]{1.0}, new double[]{196.966552}),
  Hg("Mercury", 80, new int[]{196, 198, 199, 200, 201, 202, 204},
      new double[]{0.0015, 0.0997, 0.1687, 0.2310, 0.1318, 0.2986, 0.0687},
      new double[]{195.965815, 197.966752, 198.968262, 199.968309, 200.970285, 201.970626,
          203.973476}),
  Tl("Thallium", 81, new int[]{203, 205}, new double[]{0.29524, 0.70476},
      new double[]{202.972329, 204.974412}),
  Pb("Lead", 82, new int[]{204, 206, 207, 208}, new double[]{0.014, 0.241, 0.221, 0.524},
      new double[]{203.973029, 205.974449, 206.975881, 207.976636}),
  Bi("Bismuth", 83, new int[]{209}, new double[]{1.0}, new double[]{208.980383}),
  Po("Polonium", 84, new int[]{209}, new double[]{0.0}, new double[]{208.982416}),
  At("Astatine", 85, new int[]{210}, new double[]{0.0}, new double[]{209.987131}),
  Rn("Radon", 86, new int[]{222}, new double[]{0.0}, new double[]{222.017570}),
  Fr("Francium", 87, new int[]{223}, new double[]{0.0}, new double[]{223.019731}),
  Ra("Radium", 88, new int[]{226}, new double[]{0.0}, new double[]{226.025403}),
  Ac("Actinium", 89, new int[]{227}, new double[]{0.0}, new double[]{227.027747}),
  Th("Thorium", 90, new int[]{232}, new double[]{1.0}, new double[]{232.038050}),
  Pa("Protactinium", 91, new int[]{231}, new double[]{1.0}, new double[]{231.035879}),
  U("Uranium", 92, new int[]{234, 235, 238}, new double[]{0.00055, 0.0072, 0.992745},
      new double[]{234.040946, 235.043923, 238.050783}),
  Np("Neptunium", 93, new int[]{237}, new double[]{0.0}, new double[]{237.048167}),
  Pu("Plutonium", 94, new int[]{244}, new double[]{0.0}, new double[]{244.064198}),
  Am("Americium", 95, new int[]{243}, new double[]{0.0}, new double[]{243.061373}),
  Cm("Curium", 96, new int[]{247}, new double[]{0.0}, new double[]{247.070347}),
  Bk("Berkelium", 97, new int[]{247}, new double[]{0.0}, new double[]{247.070299}),
  Cf("Californium", 98, new int[]{251}, new double[]{0.0}, new double[]{251.079580}),
  Es("Einsteinium", 99, new int[]{252}, new double[]{0.0}, new double[]{252.082972}),
  Fm("Fermium", 100, new int[]{257}, new double[]{0.0}, new double[]{257.095099}),
  Md("Mendelevium", 101, new int[]{258}, new double[]{0.0}, new double[]{258.098425}),
  No("Nobelium", 102, new int[]{259}, new double[]{0.0}, new double[]{259.101024}),
  Lr("Lawrencium", 103, new int[]{262}, new double[]{0.0}, new double[]{262.109692}),
  Rf("Rutherfordium", 104, new int[]{263}, new double[]{0.0}, new double[]{263.118313}),
  Db("Dubnium", 105, new int[]{262}, new double[]{0.0}, new double[]{262.011437}),
  Sg("Seaborgium", 106, new int[]{266}, new double[]{0.0}, new double[]{266.012238}),
  Bh("Bohrium", 107, new int[]{264}, new double[]{0.0}, new double[]{264.012496}),
  Hs("Hassium", 108, new int[]{269}, new double[]{0.0}, new double[]{269.001341}),
  Mt("Meitnerium", 109, new int[]{268}, new double[]{0.0}, new double[]{268.001388}),
  Ds("Darmstadtium", 110, new int[]{281}, new double[]{0.0}, new double[]{281.168000}),
  Rg("Roentgenium", 111, new int[]{280}, new double[]{0.0}, new double[]{280.160000}),
  Cn("Copernicium", 112, new int[]{285}, new double[]{0.0}, new double[]{285.175000}),
  Nh("Nihonium", 113, new int[]{284}, new double[]{0.0}, new double[]{284.182000}),
  Fl("Flerovium", 114, new int[]{289}, new double[]{0.0}, new double[]{289.189000}),
  Mc("Moscovium", 115, new int[]{288}, new double[]{0.0}, new double[]{288.188000}),
  Lv("Livermorium", 116, new int[]{293}, new double[]{0.0}, new double[]{293.201000}),
  Ts("Tennessine", 117, new int[]{294}, new double[]{0.0}, new double[]{294.210000}),
  Og("Oganesson", 118, new int[]{294}, new double[]{0.0}, new double[]{294.214000}),
  /// add at the end or else all colors shift by one
  UNKNOWN("Unknown", 0, new int[]{0}, new double[]{1.0}, new double[]{0});

  public static final Logger LOGGER = LogManager.getLogger(Element.class.getName());

  private final String name;
  private final int atomicNumber;
  private final int[] isotopes;
  private final double[] exactMasses;
  private final double[] abundances;

  Element(String name, int atomicNumber, int[] isotopes, double[] abundances,
          double[] exactMasses) {
    this.name = name;
    this.atomicNumber = atomicNumber;
    this.isotopes = isotopes;
    this.exactMasses = exactMasses;
    this.abundances = abundances;
  }

  public String getLongName() {
    return name;
  }

  public String getShortName() {
    return name();
  }

  public String getSymbol() {
    return toString();
  }

  public int getAtomicNumber() {
    return atomicNumber;
  }

  public double[] getAbundances() {
    return abundances;
  }

  public double calcMolarMass() {
    double molar = 0d;
    for (int i = 0; i < abundances.length; i++) {
      molar += exactMasses[i] * abundances[i];
    }
    return molar;
  }

  public List<Isotope> getIsotopes() {
    final List<Isotope> isotopeList = new ArrayList<>();
    for (int i = 0; i < isotopes.length; i++) {
      isotopeList.add(new Isotope(this, isotopes[i], exactMasses[i], abundances[i]));
    }
    return isotopeList;
  }

  public Isotope getMostAbundant() {
    List<Isotope> isotopes = getIsotopes();
    double abundance = -1; // If we chose 0, Tc returns null...
    Isotope mostAbundantIsotope = Element.UNKNOWN.getIsotopes().get(0);
    for (Isotope isotope : isotopes) {
      if (isotope.getAbundance() > abundance) {
        abundance = isotope.getAbundance();
        mostAbundantIsotope = isotope;
      }
    }
    return mostAbundantIsotope;
  }


  public HashMap<Isotope, Double> calcRandomizedIsotopeSignalLvl(double elementSignal,
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

  public HashMap<Isotope, Double> calcIsotopeSignalLvl(double elementSignal) {
    HashMap<Isotope, Double> isotopicFractions = new LinkedHashMap<>();

    List<Isotope> isotopes = getIsotopes();

    for (int i = 0; i < isotopes.size(); i++) {
      Isotope isotope = isotopes.get(i);
      double isotopeSignal = isotope.getAbundance() * elementSignal;
      isotopicFractions.put(isotopes.get(i), isotopeSignal);
    }

    return isotopicFractions;
  }

  @Nullable
  public static Element getFromString(String symbol) {
    Element element = null;
    if (symbol != null) {

      // Remove potential numbers and special characters, convert to lower case
      String cleanSymbol = symbol.replaceAll("[^a-zA-Z]", "");
      cleanSymbol = symbol.toLowerCase(Locale.ROOT);

      for (Element value : Element.values()) {
        // Au
        if (value.getShortName().toLowerCase(Locale.ROOT).equals(cleanSymbol)) {
          element = value;
          break;
          // Gold
        } else if (value.getLongName().toLowerCase(Locale.ROOT).equals(cleanSymbol)) {
          element = value;
          break;
        }
      }
    }
    return element;
  }

  @Override
  public String getStringValue() {
    return this.getShortName() + " (" + this.getLongName() + ")";
  }

  @Override
  public boolean isEqual(Fillable<?> thatFillable) {
    return this.getStringValue().equals(thatFillable.getStringValue());
  }

  @Override
  public Element unwrap() {
    return this;
  }


  @Override
  public List<Fillable<Element>> getItems() {
    return Arrays.stream(Element.values()).collect(Collectors.toList());
  }

  @Override
  public Element getMatch(String string, boolean muteError) {
    Element match = null;
    boolean parserFailed = true;
    for (Fillable<Element> item : getItems()) {
      if (string.equals(item.getStringValue())) {
        match = (Element) item; // Element implements Fillable<Element> :-
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

  public static List<Isotope> getIsotopes(List<Element> elements) {
    List<Isotope> isotopes = new ArrayList<>();
    for (Element element : elements) {
      isotopes.addAll(element.getIsotopes());
    }
    return isotopes;
  }

  public static List<Isotope> getAllIsotopes() {
    List<Isotope> isotopes = new ArrayList<>();
    for (Element element : Element.values()) {
      isotopes.addAll(element.getIsotopes());
    }
    return isotopes;
  }

  public static HashMap<Integer, Element[]> getAllConflictingIsotopicNumbers() {
    List<Isotope> isotopes = new ArrayList<>();
    for (Element element : Element.values()) {
      isotopes.addAll(element.getIsotopes());
    }
    HashMap<Integer, List<Element>> map = new LinkedHashMap<>();
    for (Isotope isotope : isotopes) {
      int nominalMass = isotope.getIsotopicNumber();
      Util.put(map, nominalMass, isotope.getElement());
    }
    map.keySet().removeIf(i -> map.get(i).size() < 2);

    HashMap<Integer, Element[]> mapArr = new LinkedHashMap<>();
    for (Integer key : map.keySet()) {
      List<Element> list = map.get(key);
      Element[] arr = new Element[list.size()];
      for (int i = 0; i < list.size(); i++) {
        arr[i] = list.get(i);
      }
      mapArr.put(key, arr);
    }
    return mapArr;
  }

  // Has some preference as to which element is first
  public static HashMap<Integer, Element[]> getAllConflictingIsotopicNumbersSorted() {
    HashMap<Integer, Element[]> map = getAllConflictingIsotopicNumbers();
    for (Integer i : map.keySet()) {
      Element[] arr = map.get(i);
      // Find priority element index
      int priorityIdx = -1;
      for (int j = 0; j < arr.length; j++) {
        if (isPriority(i, arr[j])) {
          priorityIdx = j;
          break;
        }
      }
      // Swap priority element to front if found and not already there
      if (priorityIdx > 0) {
        Element temp = arr[0];
        arr[0] = arr[priorityIdx];
        arr[priorityIdx] = temp;
      }
    }
    return map;
  }

  private static boolean isPriority(int nomIsotopicMass, Element element) {
    return switch (nomIsotopicMass) {
      case 3 -> element == H;    // He is noble gas; H trace but preferred
      case 36 -> element == S;    // Ar is noble gas
      case 40 -> element == Ca;   // Ar is noble gas; Ca 96.9% >> K 0.01%
      case 46 -> element == Ti;   // Ti 8.2% >> Ca 0.004%
      case 48 -> element == Ti;   // Ti 73.7% >> Ca 0.187%
      case 50 -> element == Ti;    // 51V is main; Ti is most abundant and wide-spread in NPs (more than Cr)
      case 54 -> element == Fe;   // Fe 5.8% > Cr 2.4%
      case 58 -> element == Ni;   // Ni 68.1% >> Fe 0.28%
      case 64 -> element == Zn;   // Zn 48.6% >> Ni 0.93%
      case 70 -> element == Ge;   // Ge 20.8% >> Zn 0.6% // Zn likely undetectable
      case 74 -> element == Ge;   // Ge 36.3% >> Se 0.87% // Se likely undetectable
      case 76 -> element == Se;   // Se 9.4% > Ge 7.6% (close, but Se more analytically relevant)
      case 78 -> element == Se;   // Se 23.8% >> Kr is noble gas
      case 80 -> element == Se;   // Se 49.6% >> Kr is noble gas
      case 82 -> element == Se;   // Kr 11.6% > Se 8.7% but Kr is noble gas
      case 84 -> element == Sr;   // Kr is noble gas
      case 86 -> element == Sr;   // Kr is noble gas
      case 87 -> element == Rb;   // Sr 7.0% vs Rb 27.8%, Sr main is 88;
      // Ru main is 85- I put Rb b/c often present with K
      case 92 -> element == Zr;   // Zr 17.2% ≈ Mo 14.8%; Zr slightly higher Zr main is 90, Mo has others
      case 94 -> element == Zr;   // Zr 17.4% >> Mo 9.2%
      case 96 -> element == Mo;   // Mo 16.7% >> Zr 2.8%, Ru 5.5% (Ru main is 102)
      case 98 -> element == Mo;   // Mo 24.1%; Tc is radioactive/no stable isotope; Ru 1.9%
      case 100 -> element == Ru;   // Ru 12.6% > Mo 9.6%
      case 102 -> element == Ru;   // Ru 31.6% >> Pd 1.0%
      case 104 -> element == Ru;   // Ru 18.6% >> Pd 11.1%  (close; Ru higher)
      case 106 -> element == Pd;   // Pd 27.3% >> Cd 1.2%
      case 108 -> element == Pd;   // Pd 26.5% >> Cd 0.9%
      case 110 -> element == Cd;   // Cd 12.5% ≈ Pd 11.7%; Cd slightly higher
      case 112 -> element == Cd;   // Cd 24.1% >> Sn 1.0%
      case 113 ->element == In;   // In 4.3% vs Cd 12.2%;
      // but In has only 2 isotopes — 113/115 both used; keep In
      case 114 -> element == Cd;   // Cd 28.7% >> Sn 0.7%
      case 115 -> element == In;   // In 95.7% >> Sn 0.3%; classic In internal standard mass
      case 116 -> element == Sn;   // Sn 14.5% >> Cd 7.5%
      case 120 -> element == Sn;   // Sn 32.6% >> Te 0.09%
      case 122 -> element == Sn;   // Te 2.5% > Sn 4.6%
      case 123 -> element == Sb;   // Sb 42.8% >> Te 0.89%
      case 124 -> element == Sn;   // Sn 5.8% > Te 4.7%; Xe is noble gas
      case 126 -> element == Te;   // Xe is noble gas
      case 128 -> element == Te;   // Te 31.7% >> Xe 1.9%
      case 130 -> element == Te;   // Te 34.1% >> Xe 4.1%, Ba 0.1%
      case 132 -> element == Ba;   // Xe 26.9% >> Ba 0.1% — but Xe is noble gas; Ba preferred
      case 134 -> element == Ba;   // Xe is noble gas; Ba 2.4%
      case 136 -> element == Ba;   // Xe is noble gas; Ba 7.9% >> Ce 0.19%
      case 138 -> element == Ba;   // Ba 71.7% >> La 0.09%, Ce 0.25%
      case 142 -> element == Nd;   // Nd 27.2% >> Ce 11.1%
      case 144 -> element == Nd;   // Nd 23.8% >> Sm 3.1%
      case 147 -> element == Sm;   // Pm is radioactive (no stable isotope); Sm 15.0%
      case 148 -> element == Sm;   // Sm 11.2% >> Nd 5.7%
      case 150 -> element == Sm;   // Sm 7.4% > Nd 5.6%
      case 152 -> element == Sm;   // Sm 26.8% >> Gd 0.2%
      case 154 -> element == Sm;   // Sm 22.8% >> Gd 2.2%
      case 156 -> element == Gd;   // Gd 20.5% >> Dy 0.06%
      case 158 -> element == Gd;   // Gd 24.8% >> Dy 0.10%
      case 160 -> element == Gd;   // Dy 2.3% vs Gd... Gd 21.9% is actually higher; swap if needed
      case 162 -> element == Dy;   // Dy 25.5% >> Er 0.14%
      case 164 -> element == Dy;   // Dy 28.2% >> Er 1.6%
      case 168 -> element == Er;   // Er 26.8% >> Yb 0.13%
      case 170 -> element == Er;   // Er 14.9% >> Yb 3.0%
      case 174 -> element == Yb;   // Yb 31.8% >> Hf 0.16%
      case 176 -> element == Yb;   // Yb 12.8% > Hf 5.3%; Lu 2.6% — Yb highest
      case 180 -> element == Hf;   // Hf 35.1% >> W 0.12%; Ta 0.012%
      case 184 -> element == W;    // W 30.6%; Os is near-zero (0.0002%)
      case 186 -> element == W;    // W 28.4% >> Os 1.6%
      case 187 -> element == Re;   // Re 62.6% >> Os 2.0%
      case 190 -> element == Os;   // Os 26.3%, Pt ~0% (Pt is more typical but not detectable at >0.1%)
      case 192 -> element == Os;   // Os 40.8% >> Pt 0.79%
      case 196 -> element == Pt;   // Pt 25.2% >> Hg 0.15%
      case 198 -> element == Hg;   // Hg 10.0% > Pt 7.2%
      case 204 -> element == Pb;   // Pb 1.4% vs Hg 6.9% — but Pb is the classic 204 mass; context-dependent
      //        -> element == Hg; // uncomment for abundance-based
      case 209 -> element == Bi;   // Bi 100%; Po is radioactive
      case 247 -> element == Bk;   // Both radioactive/synthetic; Bk slightly more stable (320d vs Cm varies)
      //        -> element == Cm; // either is defensible; Cm more commonly encountered
      case 262 -> element == Db;   // Both synthetic; arbitrary — flip if needed
      case 294 -> element == Ts;   // Both synthetic superheavies; arbitrary
      default -> false;
    };
  }


  public static String[] getAllIsotopeFullUINames() {
    List<Isotope> isotopes = getAllIsotopes();
    List<String> names = new ArrayList<>();
    for (Isotope isotope : isotopes) {
      names.add(isotope.getFullUIName());
    }
    names.sort(String::compareTo);
    return ArrUtils.stringListToArr(names);
  }

  public static String[] getAllElementNames() {
    List<String> names = Arrays.stream(dataModelNew.mz.Element.values())
        .map(dataModelNew.mz.Element::getLongName)
        .collect(Collectors.toList());
    names.sort(String::compareTo);
    return ArrUtils.stringListToArr(names);
  }

  // Do not change the labelling here!!! These are constants to find the isotopes in xml files!
  public static List<String> getAllIsotopeNamesForXML() {
    List<Isotope> isotopes = getAllIsotopes();
    List<String> names = new ArrayList<>();
    for (Isotope isotope : isotopes) {
      names.add(isotope.getXMLCode());
    }
    return names;
  }


}
