import { useState } from "react";

const AlarmToggle = () => {

    const [isAlarmOn, setIsAlarmOn] = useState(true);


    const toggleAlarm = () => {
        setIsAlarmOn((prev) => !prev);
    };

    return (

        <button
            onClick={toggleAlarm}
            className={`w-16 h-8 flex items-center px-1 rounded-full transition-all ${isAlarmOn ? "bg-purple-700" : "bg-gray-400"
                }`}
        >
            {/* 작은 원형 토글 버튼 */}
            <div
                className={`w-6 h-6 bg-white rounded-full shadow-md transform transition-all ${isAlarmOn ? "translate-x-8" : "translate-x-0"
                    }`}
            />
        </button>

    );
};

export default AlarmToggle;
