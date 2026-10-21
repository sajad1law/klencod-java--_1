```react
import React, { useState, useEffect, useMemo } from 'react';
import { Trophy, Calendar, Users, Home, Settings, ChevronRight, PlayCircle, Edit3, CheckCircle, AlertCircle } from 'lucide-react';

// --- الثوابت والبيانات الافتراضية ---
const GULF_CUP_TEAMS = [
  { id: 'ksa', name: 'السعودية', group: 'المجموعة الأولى', flag: '🇸🇦' },
  { id: 'irq', name: 'العراق', group: 'المجموعة الأولى', flag: '🇮🇶' },
  { id: 'kuw', name: 'الكويت', group: 'المجموعة الأولى', flag: '🇰🇼' },
  { id: 'oma', name: 'عمان', group: 'المجموعة الأولى', flag: '🇴🇲' },
  { id: 'bhr', name: 'البحرين', group: 'المجموعة الثانية', flag: '🇧🇭' },
  { id: 'qat', name: 'قطر', group: 'المجموعة الثانية', flag: '🇶🇦' },
  { id: 'yem', name: 'اليمن', group: 'المجموعة الثانية', flag: '🇾🇪' },
  { id: 'uae', name: 'الإمارات', group: 'المجموعة الثانية', flag: '🇦🇪' }
];

const generateGroupFixtures = (teams, groupName) => {
  const groupTeams = teams.filter(t => t.group === groupName);
  const fixtures = [];
  let matchId = 1;
  for (let i = 0; i < groupTeams.length; i++) {
    for (let j = i + 1; j < groupTeams.length; j++) {
      fixtures.push({
        id: `${groupName}-m${matchId++}-${Date.now()}`,
        team1: groupTeams[i],
        team2: groupTeams[j],
        score1: null,
        score2: null,
        isPlayed: false,
        stage: 'group',
        group: groupName,
        isSimulated: false
      });
    }
  }
  return fixtures;
};

// --- المكون الرئيسي للتطبيق ---
export default function App() {
  const [activeTab, setActiveTab] = useState('home');
  const [tournament, setTournament] = useState(null);
  const [userTeamId, setUserTeamId] = useState(null);
  
  // نافذة إدخال النتيجة
  const [scoreModal, setScoreModal] = useState({ isOpen: false, match: null, s1: '', s2: '', p1: '', p2: '' });

  // استرجاع البيانات من LocalStorage عند التحميل
  useEffect(() => {
    const saved = localStorage.getItem('tournament_data');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        setTournament(parsed.tournament);
        setUserTeamId(parsed.userTeamId);
      } catch (e) {
        console.error("Error loading data", e);
      }
    }
  }, []);

  // حفظ البيانات عند أي تغيير
  useEffect(() => {
    if (tournament) {
      localStorage.setItem('tournament_data', JSON.stringify({ tournament, userTeamId }));
    }
  }, [tournament, userTeamId]);

  // --- محرك المحاكاة ---
  const realisticScore = () => {
    const weights = [0, 0, 1, 1, 1, 2, 2, 3, 4];
    return weights[Math.floor(Math.random() * weights.length)];
  };

  const simulateMatch = (match) => {
    return {
      ...match,
      score1: realisticScore(),
      score2: realisticScore(),
      isPlayed: true,
      isSimulated: true
    };
  };

  const simulateAllPending = () => {
    setTournament(prev => {
      const newFixtures = prev.fixtures.map(m => {
        if (!m.isPlayed && m.team1.id !== userTeamId && m.team2.id !== userTeamId) {
          return simulateMatch(m);
        }
        return m;
      });
      return { ...prev, fixtures: newFixtures };
    });
    checkAndGenerateKnockouts();
  };

  // --- حساب الترتيب ---
  const getStandings = (groupName) => {
    if (!tournament) return [];
    
    let standings = tournament.teams
      .filter(t => t.group === groupName)
      .map(t => ({ ...t, p: 0, w: 0, d: 0, l: 0, gf: 0, ga: 0, gd: 0, pts: 0 }));

    tournament.fixtures.filter(m => m.group === groupName && m.isPlayed).forEach(m => {
      let t1 = standings.find(s => s.id === m.team1.id);
      let t2 = standings.find(s => s.id === m.team2.id);

      t1.p++; t2.p++;
      t1.gf += m.score1; t1.ga += m.score2;
      t2.gf += m.score2; t2.ga += m.score1;

      if (m.score1 > m.score2) { t1.w++; t1.pts += 3; t2.l++; }
      else if (m.score1 < m.score2) { t2.w++; t2.pts += 3; t1.l++; }
      else { t1.d++; t2.d++; t1.pts += 1; t2.pts += 1; }
    });

    standings.forEach(s => s.gd = s.gf - s.ga);

    // ترتيب: نقاط > فارق أهداف > أهداف له
    standings.sort((a, b) => {
      if (b.pts !== a.pts) return b.pts - a.pts;
      if (b.gd !== a.gd) return b.gd - a.gd;
      return b.gf - a.gf;
    });

    return standings;
  };

  // --- إنشاء الأدوار الإقصائية ---
  const checkAndGenerateKnockouts = () => {
    setTournament(prev => {
      if (!prev) return prev;
      
      const groupMatches = prev.fixtures.filter(m => m.stage === 'group');
      const allGroupPlayed = groupMatches.every(m => m.isPlayed);
      const hasKnockouts = prev.fixtures.some(m => m.stage === 'semi');

      if (allGroupPlayed && !hasKnockouts) {
        const standingsA = getStandings('المجموعة الأولى');
        const standingsB = getStandings('المجموعة الثانية');

        const semi1 = {
          id: `semi-1-${Date.now()}`,
          team1: standingsA[0],
          team2: standingsB[1],
          score1: null, score2: null, penalties1: null, penalties2: null,
          isPlayed: false, stage: 'semi', group: 'نصف النهائي 1'
        };
        const semi2 = {
          id: `semi-2-${Date.now()}`,
          team1: standingsB[0],
          team2: standingsA[1],
          score1: null, score2: null, penalties1: null, penalties2: null,
          isPlayed: false, stage: 'semi', group: 'نصف النهائي 2'
        };

        return { ...prev, fixtures: [...prev.fixtures, semi1, semi2], stage: 'نصف النهائي' };
      }

      // التحقق من النهائي
      const semiMatches = prev.fixtures.filter(m => m.stage === 'semi');
      const allSemiPlayed = semiMatches.length === 2 && semiMatches.every(m => m.isPlayed);
      const hasFinal = prev.fixtures.some(m => m.stage === 'final');

      if (allSemiPlayed && !hasFinal) {
        const getWinner = (m) => {
           if (m.score1 > m.score2) return m.team1;
           if (m.score2 > m.score1) return m.team2;
           if (m.penalties1 > m.penalties2) return m.team1;
           return m.team2;
        };
        
        const final = {
          id: `final-${Date.now()}`,
          team1: getWinner(semiMatches[0]),
          team2: getWinner(semiMatches[1]),
          score1: null, score2: null, penalties1: null, penalties2: null,
          isPlayed: false, stage: 'final', group: 'المباراة النهائية'
        };
        return { ...prev, fixtures: [...prev.fixtures, final], stage: 'النهائي' };
      }

      return prev;
    });
  };

  // --- إجراءات المستخدم ---
  const startNewTournament = (teamId) => {
    const fixturesA = generateGroupFixtures(GULF_CUP_TEAMS, 'المجموعة الأولى');
    const fixturesB = generateGroupFixtures(GULF_CUP_TEAMS, 'المجموعة الثانية');
    
    setTournament({
      name: 'كأس الخليج العربي',
      host: 'السعودية',
      year: '2026',
      edition: 'النسخة 27',
      stage: 'دور المجموعات',
      teams: GULF_CUP_TEAMS,
      fixtures: [...fixturesA, ...fixturesB]
    });
    setUserTeamId(teamId);
    setActiveTab('home');
  };

  const handleSaveScore = () => {
    const { match, s1, s2, p1, p2 } = scoreModal;
    const numS1 = parseInt(s1);
    const numS2 = parseInt(s2);
    
    if (isNaN(numS1) || isNaN(numS2)) {
      alert("الرجاء إدخال أرقام صحيحة");
      return;
    }

    if (match.stage !== 'group' && numS1 === numS2 && (p1 === '' || p2 === '')) {
      alert("مباريات خروج المغلوب تتطلب إدخال نتيجة ركلات الترجيح في حال التعادل");
      return;
    }

    setTournament(prev => {
      const newFixtures = prev.fixtures.map(m => {
        if (m.id === match.id) {
          return { ...m, score1: numS1, score2: numS2, penalties1: p1 ? parseInt(p1) : null, penalties2: p2 ? parseInt(p2) : null, isPlayed: true, isSimulated: false };
        }
        return m;
      });
      return { ...prev, fixtures: newFixtures };
    });

    setScoreModal({ isOpen: false, match: null, s1: '', s2: '', p1: '', p2: '' });
    setTimeout(checkAndGenerateKnockouts, 100);
  };

  const resetApp = () => {
    if(window.confirm('هل أنت متأكد من حذف البطولة الحالية؟')) {
      localStorage.removeItem('tournament_data');
      setTournament(null);
      setUserTeamId(null);
    }
  };

  // --- واجهة اختيار المنتخب (عند البدء) ---
  if (!tournament) {
    return (
      <div dir="rtl" className="min-h-screen bg-slate-900 text-white font-sans p-6 flex flex-col items-center justify-center">
        <style>{`@import url('https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap'); * { font-family: 'Cairo', sans-serif; }`}</style>
        
        <div className="bg-slate-800 p-8 rounded-2xl shadow-2xl max-w-2xl w-full border border-slate-700 text-center">
          <Trophy className="w-20 h-20 text-yellow-400 mx-auto mb-4" />
          <h1 className="text-4xl font-bold mb-2 text-emerald-400">مدير البطولات الدولية</h1>
          <p className="text-slate-400 mb-8">اختر المنتخب الذي ستلعب مبارياته داخل اللعبة. ستقوم بإدخال نتائج هذا المنتخب يدوياً، بينما سيحاكي النظام باقي المباريات بواقعية.</p>
          
          <div className="bg-slate-900 rounded-xl p-4 mb-6">
            <h2 className="text-xl font-semibold mb-4 border-b border-slate-700 pb-2">قالب البطولة: كأس الخليج العربي 2026</h2>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {GULF_CUP_TEAMS.map(team => (
                <button 
                  key={team.id}
                  onClick={() => startNewTournament(team.id)}
                  className="bg-slate-800 hover:bg-slate-700 border border-slate-600 rounded-xl p-4 flex flex-col items-center transition transform hover:scale-105"
                >
                  <span className="text-4xl mb-2">{team.flag}</span>
                  <span className="font-bold">{team.name}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  // --- البيانات المحسوبة للواجهات ---
  const userTeam = tournament.teams.find(t => t.id === userTeamId);
  const nextUserMatch = tournament.fixtures.find(m => (m.team1.id === userTeamId || m.team2.id === userTeamId) && !m.isPlayed);
  const totalMatches = tournament.fixtures.length;
  const playedMatches = tournament.fixtures.filter(m => m.isPlayed).length;

  return (
    <div dir="rtl" className="min-h-screen bg-slate-900 text-white font-sans pb-24">
      <style>{`@import url('https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap'); * { font-family: 'Cairo', sans-serif; }`}</style>
      
      {/* Header */}
      <header className="bg-slate-800 shadow-md p-4 sticky top-0 z-10 border-b border-slate-700 flex justify-between items-center">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-emerald-600 rounded-full flex items-center justify-center">
            <Trophy className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="font-bold text-lg leading-tight">{tournament.name} {tournament.year}</h1>
            <p className="text-xs text-slate-400">{tournament.host} - {tournament.edition}</p>
          </div>
        </div>
        <button onClick={resetApp} className="text-red-400 hover:text-red-300 text-sm flex items-center gap-1">
          <Settings className="w-4 h-4" /> إنهاء
        </button>
      </header>

      {/* Main Content */}
      <main className="p-4 max-w-4xl mx-auto">
        
        {/* --- الرئيسية --- */}
        {activeTab === 'home' && (
          <div className="space-y-6">
            <div className="bg-gradient-to-br from-emerald-800 to-slate-800 rounded-2xl p-6 shadow-xl border border-emerald-900/50">
              <h2 className="text-sm font-bold text-emerald-200 mb-1">منتخبك المختار</h2>
              <div className="flex items-center gap-4 mb-6">
                <span className="text-6xl">{userTeam.flag}</span>
                <div>
                  <h3 className="text-3xl font-black">{userTeam.name}</h3>
                  <p className="text-emerald-100">المرحلة الحالية: {tournament.stage}</p>
                </div>
              </div>
              
              <div className="bg-slate-900/50 rounded-xl p-4">
                <h4 className="font-semibold mb-3 flex items-center gap-2">
                  <Calendar className="w-5 h-5 text-emerald-400" /> المباراة القادمة لمنتخبك
                </h4>
                {nextUserMatch ? (
                  <div className="flex items-center justify-between bg-slate-800 p-4 rounded-lg border border-slate-700">
                    <div className="flex items-center gap-3 w-1/3 justify-end">
                      <span className="font-bold text-lg">{nextUserMatch.team1.name}</span>
                      <span className="text-3xl">{nextUserMatch.team1.flag}</span>
                    </div>
                    <div className="text-slate-400 font-bold px-4">ضد</div>
                    <div className="flex items-center gap-3 w-1/3 justify-start">
                      <span className="text-3xl">{nextUserMatch.team2.flag}</span>
                      <span className="font-bold text-lg">{nextUserMatch.team2.name}</span>
                    </div>
                  </div>
                ) : (
                  <div className="text-center text-slate-400 p-4">لا توجد مباريات متبقية لمنتخبك حالياً</div>
                )}
                
                {nextUserMatch && (
                  <button 
                    onClick={() => setScoreModal({ isOpen: true, match: nextUserMatch, s1: '', s2: '', p1: '', p2: '' })}
                    className="w-full mt-4 bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-3 rounded-lg shadow flex items-center justify-center gap-2 transition"
                  >
                    <Edit3 className="w-5 h-5" /> إدخال نتيجة المباراة
                  </button>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="bg-slate-800 p-5 rounded-2xl border border-slate-700 flex flex-col items-center justify-center">
                <span className="text-4xl font-black text-emerald-400">{playedMatches} / {totalMatches}</span>
                <span className="text-sm text-slate-400 mt-2">المباريات المكتملة</span>
              </div>
              <div className="bg-slate-800 p-5 rounded-2xl border border-slate-700 flex flex-col items-center justify-center">
                 <button onClick={simulateAllPending} className="flex flex-col items-center text-blue-400 hover:text-blue-300">
                   <PlayCircle className="w-10 h-10 mb-2" />
                   <span className="font-bold">محاكاة باقي الجولة</span>
                 </button>
              </div>
            </div>
          </div>
        )}

        {/* --- المجموعات --- */}
        {activeTab === 'groups' && (
          <div className="space-y-6">
            {['المجموعة الأولى', 'المجموعة الثانية'].map(group => {
              const standings = getStandings(group);
              return (
                <div key={group} className="bg-slate-800 rounded-2xl overflow-hidden border border-slate-700 shadow-lg">
                  <div className="bg-slate-700 p-3 font-bold text-center">{group}</div>
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm text-center">
                      <thead className="bg-slate-800 text-slate-400 border-b border-slate-700">
                        <tr>
                          <th className="p-3 text-right">المنتخب</th>
                          <th className="p-3">لعب</th>
                          <th className="p-3">فاز</th>
                          <th className="p-3">تعادل</th>
                          <th className="p-3">خسر</th>
                          <th className="p-3">فارق</th>
                          <th className="p-3 font-black text-emerald-400">نقاط</th>
                        </tr>
                      </thead>
                      <tbody>
                        {standings.map((team, idx) => (
                          <tr key={team.id} className={`border-b border-slate-700/50 ${team.id === userTeamId ? 'bg-emerald-900/30 font-bold' : ''} ${idx < 2 ? 'border-l-4 border-l-emerald-500' : ''}`}>
                            <td className="p-3 flex items-center gap-2 text-right">
                              <span>{team.flag}</span>
                              {team.name}
                            </td>
                            <td className="p-3">{team.p}</td>
                            <td className="p-3">{team.w}</td>
                            <td className="p-3">{team.d}</td>
                            <td className="p-3">{team.l}</td>
                            <td className="p-3">{team.gd > 0 ? `+${team.gd}` : team.gd}</td>
                            <td className="p-3 font-black text-emerald-400">{team.pts}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )
            })}
          </div>
        )}

        {/* --- جدول المباريات --- */}
        {activeTab === 'fixtures' && (
          <div className="space-y-4">
            {tournament.fixtures.map((match) => {
              const isUserMatch = match.team1.id === userTeamId || match.team2.id === userTeamId;
              return (
                <div key={match.id} className={`bg-slate-800 rounded-xl p-4 border ${isUserMatch ? 'border-emerald-500 shadow-[0_0_10px_rgba(16,185,129,0.2)]' : 'border-slate-700'}`}>
                  <div className="text-xs text-slate-400 text-center mb-3">{match.group}</div>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 w-1/3 justify-end">
                      <span className={`font-bold ${match.score1 > match.score2 ? 'text-emerald-400' : ''}`}>{match.team1.name}</span>
                      <span className="text-2xl">{match.team1.flag}</span>
                    </div>
                    
                    <div className="w-1/3 flex flex-col items-center">
                      {match.isPlayed ? (
                        <div className="bg-slate-900 px-4 py-2 rounded-lg font-black text-xl tracking-widest border border-slate-700">
                          {match.score1} - {match.score2}
                        </div>
                      ) : (
                        isUserMatch ? (
                          <button onClick={() => setScoreModal({ isOpen: true, match, s1: '', s2: '', p1: '', p2: '' })} className="bg-emerald-600 text-xs px-3 py-1.5 rounded text-white font-bold hover:bg-emerald-500">
                            إدخال النتيجة
                          </button>
                        ) : (
                          <button onClick={() => setTournament(prev => ({ ...prev, fixtures: prev.fixtures.map(m => m.id === match.id ? simulateMatch(m) : m) }))} className="bg-slate-700 text-xs px-3 py-1.5 rounded text-slate-300 hover:bg-slate-600">
                            محاكاة
                          </button>
                        )
                      )}
                      
                      {match.isPlayed && (match.penalties1 !== null || match.penalties2 !== null) && (
                        <div className="text-xs text-slate-400 mt-1">
                          (ترجيح: {match.penalties1} - {match.penalties2})
                        </div>
                      )}
                      
                      {match.isSimulated && <span className="text-[10px] text-slate-500 mt-1">تمت المحاكاة</span>}
                    </div>

                    <div className="flex items-center gap-2 w-1/3 justify-start">
                      <span className="text-2xl">{match.team2.flag}</span>
                      <span className={`font-bold ${match.score2 > match.score1 ? 'text-emerald-400' : ''}`}>{match.team2.name}</span>
                    </div>
                  </div>
                </div>
              )
            })}
        